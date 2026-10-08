package net.kroet.turtlelib.UpdateChecker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kroet.turtlelib.VersionComparator.VersionComparator;

/**
 * Loader-neutral update checker for Modrinth (official API) and CurseForge (via
 * the third-party service CFWidget, api.cfwidget.com).
 * <p>
 * {@link #check(String)} performs blocking network requests: call it from a
 * background thread, never from the game's main thread.
 */
public final class UpdateChecker {
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/";
    private static final String MODRINTH_SITE = "https://modrinth.com/project/";
    private static final String CFWIDGET_API = "https://api.cfwidget.com/";
    // First x.y.z (with optional pre-release) in a file name such as
    // "PermaClicker_2.0.0_MC-26-3_FABRIC_0-19-5.jar".
    private static final Pattern FILE_NAME_VERSION = Pattern.compile("(?<![\\d.])(\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z]+(?:\\.[0-9A-Za-z]+)*)?)");

    private final String userAgent;
    private final String modrinthProjectId;
    private final String curseforgeProjectId;
    private final UpdateReleaseChannel releaseChannel;
    private final String minecraftVersion;
    private final UpdateLoader loader;
    private final HttpGet http;

    /**
     * @param userAgent
     *            identifying User-Agent, e.g.
     *            {@code "MyMod/1.0.0 (+https://example.com)"}
     * @param modrinthProjectId
     *            Modrinth project id or slug; null skips Modrinth
     * @param curseforgeProjectId
     *            numeric CurseForge project id; null skips CurseForge (CFWidget
     *            slug paths can answer with a cached 404)
     * @param releaseChannel
     *            lowest release type to report; null means
     *            {@link UpdateReleaseChannel#BETA}
     * @param minecraftVersion
     *            exact Minecraft version a release must support
     * @param loader
     *            loader a release must support
     */
    public UpdateChecker(
            String userAgent,
            String modrinthProjectId,
            String curseforgeProjectId,
            UpdateReleaseChannel releaseChannel,
            String minecraftVersion,
            UpdateLoader loader) {
        this(userAgent, modrinthProjectId, curseforgeProjectId, releaseChannel, minecraftVersion, loader, new JdkHttpGet());
    }

    UpdateChecker(
            String userAgent,
            String modrinthProjectId,
            String curseforgeProjectId,
            UpdateReleaseChannel releaseChannel,
            String minecraftVersion,
            UpdateLoader loader,
            HttpGet http) {
        this.userAgent = userAgent;
        this.modrinthProjectId = blankToNull(modrinthProjectId);
        this.curseforgeProjectId = blankToNull(curseforgeProjectId);
        this.releaseChannel = releaseChannel == null ? UpdateReleaseChannel.BETA : releaseChannel;
        this.minecraftVersion = blankToNull(minecraftVersion);
        this.loader = loader;
        this.http = http;
    }

    public UpdateResult check(String currentVersion) {
        Map<UpdatePlatform, UpdateRelease> releases = new EnumMap<>(UpdatePlatform.class);
        Map<UpdatePlatform, String> errors = new EnumMap<>(UpdatePlatform.class);
        if (modrinthProjectId != null) {
            query(UpdatePlatform.MODRINTH, releases, errors);
        }
        if (curseforgeProjectId != null) {
            query(UpdatePlatform.CURSEFORGE, releases, errors);
        }

        String latest = null;
        for (UpdateRelease release : releases.values()) {
            if (VersionComparator.isGreater(release.version(), currentVersion)
                    && (latest == null || VersionComparator.isGreater(release.version(), latest))) {
                latest = release.version();
            }
        }
        return new UpdateResult(currentVersion, latest, releases, errors);
    }

    private void query(UpdatePlatform platform, Map<UpdatePlatform, UpdateRelease> releases, Map<UpdatePlatform, String> errors) {
        try {
            UpdateRelease release = switch (platform) {
                case MODRINTH -> newestModrinthRelease(JsonParser.parseString(http.get(modrinthUrl(), userAgent)));
                case CURSEFORGE -> newestCurseForgeRelease(JsonParser.parseString(http.get(curseForgeUrl(), userAgent)));
            };
            if (release == null) {
                errors.put(platform, "no matching release");
            } else {
                releases.put(platform, release);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errors.put(platform, "interrupted");
        } catch (Exception e) {
            String message = e.getMessage();
            errors.put(platform, e.getClass().getSimpleName() + (message == null ? "" : ": " + message));
        }
    }

    private String modrinthUrl() {
        StringBuilder url = new StringBuilder(MODRINTH_API)
                .append(encode(modrinthProjectId))
                .append("/version?include_changelog=false");
        if (loader != null) {
            url.append("&loaders=").append(encode("[\"" + loader.modrinthId() + "\"]"));
        }
        if (minecraftVersion != null) {
            url.append("&game_versions=").append(encode("[\"" + minecraftVersion + "\"]"));
        }
        return url.toString();
    }

    private String curseForgeUrl() {
        return CFWIDGET_API + curseforgeProjectId;
    }

    private UpdateRelease newestModrinthRelease(JsonElement root) {
        if (!root.isJsonArray()) {
            return null;
        }
        UpdateRelease newest = null;
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject version = element.getAsJsonObject();
            String number = string(version, "version_number");
            if (number == null
                    || !allowedByChannel(string(version, "version_type"))
                    || (loader != null && !containsIgnoreCase(version.get("loaders"), loader.modrinthId()))
                    || (minecraftVersion != null && !containsIgnoreCase(version.get("game_versions"), minecraftVersion))) {
                continue;
            }
            if (newest == null || VersionComparator.isGreater(number, newest.version())) {
                String projectId = string(version, "project_id");
                String versionId = string(version, "id");
                String url = projectId == null || versionId == null ? null : MODRINTH_SITE + projectId + "/version/" + versionId;
                newest = new UpdateRelease(number, url);
            }
        }
        return newest;
    }

    private UpdateRelease newestCurseForgeRelease(JsonElement root) {
        if (!root.isJsonObject() || !root.getAsJsonObject().has("files") || !root.getAsJsonObject().get("files").isJsonArray()) {
            return null;
        }
        UpdateRelease newest = null;
        for (JsonElement element : root.getAsJsonObject().getAsJsonArray("files")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject file = element.getAsJsonObject();
            if (!allowedByChannel(string(file, "type"))
                    || (loader != null && !containsIgnoreCase(file.get("versions"), loader.curseForgeTag()))
                    || (minecraftVersion != null && !containsIgnoreCase(file.get("versions"), minecraftVersion))) {
                continue;
            }
            String number = versionFromFileName(string(file, "name"));
            if (number == null) {
                number = versionFromFileName(string(file, "display"));
            }
            if (number != null && (newest == null || VersionComparator.isGreater(number, newest.version()))) {
                newest = new UpdateRelease(number, string(file, "url"));
            }
        }
        return newest;
    }

    static String versionFromFileName(String name) {
        if (name == null) {
            return null;
        }
        Matcher matcher = FILE_NAME_VERSION.matcher(name);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean allowedByChannel(String type) {
        String normalized = type == null ? "release" : type.trim().toLowerCase(Locale.ROOT);
        return switch (releaseChannel) {
            case ALPHA -> true;
            case BETA -> !"alpha".equals(normalized);
            case STABLE -> "release".equals(normalized);
        };
    }

    private static boolean containsIgnoreCase(JsonElement array, String expected) {
        if (array == null || !array.isJsonArray()) {
            return false;
        }
        JsonArray values = array.getAsJsonArray();
        for (JsonElement value : values) {
            if (value.isJsonPrimitive() && value.getAsString().trim().equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? blankToNull(value.getAsString()) : null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
