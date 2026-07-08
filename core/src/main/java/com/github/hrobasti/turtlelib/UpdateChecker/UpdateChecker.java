package com.github.hrobasti.turtlelib.UpdateChecker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.hrobasti.turtlelib.VersionComparator.VersionComparator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loader-neutral update checker for Modrinth and CurseForge.
 */
public final class UpdateChecker {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final String userAgent;
    private final String modrinthProjectId;
    private final String curseforgeProjectId;
    private final boolean includePrereleases;
    private final UpdateReleaseChannel releaseChannel;
    private final boolean filterByMinecraftVersion;
    private final String minecraftVersion;
    private final String minecraftVersionLower;
    private final String minecraftVersionPrefixLower;

    public UpdateChecker(String userAgent, String modrinthProjectId, String curseforgeProjectId) {
        this(userAgent, modrinthProjectId, curseforgeProjectId, false, false, null);
    }

    public UpdateChecker(
        String userAgent,
        String modrinthProjectId,
        String curseforgeProjectId,
        boolean includePrereleases,
        boolean filterByMinecraftVersion,
        String minecraftVersion
    ) {
        this.userAgent = userAgent;
        this.modrinthProjectId = modrinthProjectId;
        this.curseforgeProjectId = curseforgeProjectId;
        this.includePrereleases = includePrereleases;
        this.releaseChannel = null;
        this.filterByMinecraftVersion = filterByMinecraftVersion;
        this.minecraftVersion = normalizeMinecraftVersion(minecraftVersion);
        this.minecraftVersionLower = this.minecraftVersion == null ? null : this.minecraftVersion.toLowerCase(Locale.ROOT);
        this.minecraftVersionPrefixLower = this.minecraftVersion == null
            ? null
            : deriveMinorPrefix(this.minecraftVersion).toLowerCase(Locale.ROOT);
    }

    public UpdateChecker(
        String userAgent,
        String modrinthProjectId,
        String curseforgeProjectId,
        UpdateReleaseChannel releaseChannel,
        boolean filterByMinecraftVersion,
        String minecraftVersion
    ) {
        this.userAgent = userAgent;
        this.modrinthProjectId = modrinthProjectId;
        this.curseforgeProjectId = curseforgeProjectId;
        this.includePrereleases = true;
        this.releaseChannel = releaseChannel == null ? UpdateReleaseChannel.BETA : releaseChannel;
        this.filterByMinecraftVersion = filterByMinecraftVersion;
        this.minecraftVersion = normalizeMinecraftVersion(minecraftVersion);
        this.minecraftVersionLower = this.minecraftVersion == null ? null : this.minecraftVersion.toLowerCase(Locale.ROOT);
        this.minecraftVersionPrefixLower = this.minecraftVersion == null
            ? null
            : deriveMinorPrefix(this.minecraftVersion).toLowerCase(Locale.ROOT);
    }

    public UpdateResult check(String currentVersion) {
        Map<UpdatePlatform, String> versions = new EnumMap<>(UpdatePlatform.class);
        versions.put(UpdatePlatform.MODRINTH, fetchModrinthVersion());
        versions.put(UpdatePlatform.CURSEFORGE, fetchCurseForgeVersion());

        String latest = null;
        for (String candidate : versions.values()) {
            if (candidate == null || !VersionComparator.isGreater(candidate, currentVersion)) {
                continue;
            }
            if (latest == null || VersionComparator.isGreater(candidate, latest)) {
                latest = candidate;
            }
        }

        return new UpdateResult(currentVersion, latest, Map.copyOf(versions));
    }

    private String fetchModrinthVersion() {
        if (modrinthProjectId == null || modrinthProjectId.isBlank()) {
            return null;
        }

        String url = "https://api.modrinth.com/v2/project/" + modrinthProjectId + "/version";
        try {
            JsonElement root = fetchJson(url);
            if (!root.isJsonArray()) {
                return null;
            }

            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (!isAllowedByModrinthChannel(obj)) {
                    continue;
                }
                if (!matchesMinecraftVersion(obj)) {
                    continue;
                }
                if (obj.has("version_number")) {
                    return obj.get("version_number").getAsString();
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String fetchCurseForgeVersion() {
        if (curseforgeProjectId == null || curseforgeProjectId.isBlank()) {
            return null;
        }

        String url = "https://api.cfwidget.com/minecraft/mc-mods/" + curseforgeProjectId;
        try {
            JsonElement root = fetchJson(url);
            if (!root.isJsonObject()) {
                return null;
            }

            JsonObject obj = root.getAsJsonObject();
            if (!obj.has("files") || !obj.get("files").isJsonObject()) {
                return null;
            }

            JsonObject files = obj.getAsJsonObject("files");
            if (!files.has("latest") || !files.get("latest").isJsonObject()) {
                return null;
            }

            JsonObject latest = files.getAsJsonObject("latest");
            if (latest.has("display")) {
                String value = latest.get("display").getAsString();
                if (!isAllowedByDisplayChannel(value)) {
                    return null;
                }
                return value;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private boolean isModrinthPrerelease(JsonObject version) {
        if (version.has("prerelease") && version.get("prerelease").isJsonPrimitive()) {
            try {
                if (version.get("prerelease").getAsBoolean()) {
                    return true;
                }
            } catch (Exception ignored) {
                // ignore parse issue
            }
        }
        if (version.has("version_type") && version.get("version_type").isJsonPrimitive()) {
            String type = version.get("version_type").getAsString();
            return !"release".equalsIgnoreCase(type);
        }
        return false;
    }

    private boolean isAllowedByModrinthChannel(JsonObject version) {
        if (releaseChannel == null) {
            return includePrereleases || !isModrinthPrerelease(version);
        }

        return switch (releaseChannel) {
            case ALPHA -> true;
            case BETA -> !isModrinthAlpha(version);
            case STABLE -> !isModrinthPrerelease(version);
        };
    }

    private boolean isAllowedByDisplayChannel(String value) {
        if (releaseChannel == null) {
            return includePrereleases || !isLikelyPrereleaseTag(value);
        }

        return switch (releaseChannel) {
            case ALPHA -> true;
            case BETA -> !isLikelyAlphaTag(value);
            case STABLE -> !isLikelyPrereleaseTag(value);
        };
    }

    private static boolean isModrinthAlpha(JsonObject version) {
        String versionType = "";
        if (version.has("version_type") && version.get("version_type").isJsonPrimitive()) {
            versionType = version.get("version_type").getAsString();
        }
        String normalizedType = versionType == null ? "" : versionType.trim().toLowerCase(Locale.ROOT);
        if ("alpha".equals(normalizedType)) {
            return true;
        }

        String versionNumber = version.has("version_number") && version.get("version_number").isJsonPrimitive()
            ? version.get("version_number").getAsString()
            : "";
        String name = version.has("name") && version.get("name").isJsonPrimitive()
            ? version.get("name").getAsString()
            : "";

        return isLikelyAlphaTag(versionNumber) || isLikelyAlphaTag(name);
    }

    private boolean matchesMinecraftVersion(JsonObject version) {
        if (!filterByMinecraftVersion || minecraftVersionLower == null) {
            return true;
        }

        JsonElement gameVersions = version.get("game_versions");
        if (gameVersions != null && gameVersions.isJsonArray()) {
            for (JsonElement item : gameVersions.getAsJsonArray()) {
                if (item.isJsonPrimitive() && matchesVersionToken(item.getAsString())) {
                    return true;
                }
            }
            return false;
        }

        return fallbackMatches(version.toString());
    }

    private boolean matchesVersionToken(String token) {
        if (token == null || minecraftVersionLower == null) {
            return false;
        }
        String lower = token.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return false;
        }
        if (lower.equals(minecraftVersionLower)) {
            return true;
        }
        if (minecraftVersionPrefixLower == null) {
            return false;
        }
        return lower.equals(minecraftVersionPrefixLower)
            || lower.startsWith(minecraftVersionPrefixLower + ".")
            || lower.equals(minecraftVersionPrefixLower + ".x")
            || lower.equals(minecraftVersionPrefixLower + "x");
    }

    private boolean fallbackMatches(String rawJson) {
        if (rawJson == null || minecraftVersion == null) {
            return false;
        }
        return rawJson.contains('"' + minecraftVersion + '"')
            || rawJson.contains(minecraftVersion)
            || (minecraftVersionPrefixLower != null && rawJson.toLowerCase(Locale.ROOT).contains(minecraftVersionPrefixLower));
    }

    private static String normalizeMinecraftVersion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int dashIndex = trimmed.indexOf('-');
        if (dashIndex > 0) {
            trimmed = trimmed.substring(0, dashIndex);
        }
        return trimmed;
    }

    private static String deriveMinorPrefix(String version) {
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return version;
    }

    private static boolean isLikelyPrereleaseTag(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.contains("alpha") || lower.contains("beta") || lower.contains("rc");
    }

    private static boolean isLikelyAlphaTag(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.contains("alpha");
    }

    private JsonElement fetchJson(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", userAgent);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + " for " + url);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return JsonParser.parseString(sb.toString());
        }
    }
}

