package com.github.hrobasti.turtlelib.MessageService;

import com.github.hrobasti.turtlelib.LangLoader.LangLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * General localization helper for Fabric/NeoForge based on TurtleLib LangLoader.
 *
 * <p>This is the mod-environment counterpart to the Paper MessageService. Since Bukkit and Adventure
 * are not available here, it returns formatted strings that can be forwarded to platform chat APIs.</p>
 */
public class MessageService {
    private static final Pattern MINI_TAG_PATTERN = Pattern.compile("</?[^>]+>");

    private final Class<?> resourceAnchor;
    private final Path langDir;
    private final Map<String, String> messages = new HashMap<>();
    private final String defaultPrefixRaw;
    private final String defaultPrefixLabel;

    private String prefixRaw;
    private String prefixLabel;
    private String currentLocale = LangLoader.DEFAULT_LOCALE;

    public MessageService(Class<?> resourceAnchor, Path langDir, String defaultPrefixRaw, String defaultPrefixLabel) {
        this.resourceAnchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");
        this.langDir = Objects.requireNonNull(langDir, "langDir");
        this.defaultPrefixRaw = defaultPrefixRaw != null && !defaultPrefixRaw.isBlank()
            ? defaultPrefixRaw
            : "[<prefix_label>]";
        this.defaultPrefixLabel = defaultPrefixLabel != null && !defaultPrefixLabel.isBlank()
            ? defaultPrefixLabel
            : resourceAnchor.getSimpleName();
        this.prefixRaw = this.defaultPrefixRaw;
        this.prefixLabel = this.defaultPrefixLabel;
    }

    public static List<String> getBundledLocales(Class<?> resourceAnchor) {
        return LangLoader.getBundledLocales(resourceAnchor);
    }

    public void load(String locale) {
        messages.clear();
        String normalized = normalize(locale);
        currentLocale = normalized;
        Properties props = LangLoader.loadLocale(resourceAnchor, langDir, normalized);
        collectMessages(props);
        this.prefixRaw = messages.getOrDefault("ui.prefix", defaultPrefixRaw);
    }

    public List<String> syncLocaleFile(String locale) {
        return LangLoader.syncLocale(resourceAnchor, langDir, locale);
    }

    public String getLanguage() {
        return currentLocale;
    }

    public void setPrefixLabel(String label) {
        this.prefixLabel = (label == null || label.isBlank()) ? defaultPrefixLabel : label;
    }

    public String component(String key) {
        String raw = messages.getOrDefault(key, key);
        return applyResolvers(raw, Map.of());
    }

    public String format(String key, Map<String, String> replacements) {
        String raw = messages.getOrDefault(key, key);
        return applyResolvers(raw, replacements == null ? Map.of() : replacements);
    }

    public String plain(String key) {
        return stripMiniTags(component(key));
    }

    public String plain(String key, Map<String, String> replacements) {
        if (replacements == null || replacements.isEmpty()) {
            return plain(key);
        }
        return stripMiniTags(format(key, replacements));
    }

    public void reload(Properties config) {
        if (config == null) {
            load(currentLocale);
            return;
        }
        String lang = config.getProperty("language", currentLocale);
        load(lang);
    }

    public void reload(Map<String, ?> config) {
        if (config == null) {
            load(currentLocale);
            return;
        }
        Object language = config.get("language");
        load(language == null ? currentLocale : String.valueOf(language));
    }

    private void collectMessages(Properties properties) {
        for (String key : properties.stringPropertyNames()) {
            messages.put(key, properties.getProperty(key, key));
        }
    }

    private static String normalize(String locale) {
        if (locale == null || locale.isBlank()) {
            return LangLoader.DEFAULT_LOCALE;
        }
        String trimmed = locale.trim();
        if (trimmed.endsWith(".properties")) {
            return trimmed.substring(0, trimmed.length() - ".properties".length());
        }
        if (trimmed.endsWith(".yml")) {
            return trimmed.substring(0, trimmed.length() - ".yml".length());
        }
        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            if (parts.length == 2) {
                return parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT);
            }
        }
        return trimmed;
    }

    private String applyResolvers(String raw, Map<String, String> replacements) {
        String renderedPrefix = prefixRaw.replace("<prefix_label>", prefixLabel);
        String result = raw
            .replace("<prefix>", renderedPrefix)
            .replace("<prefix_label>", prefixLabel);

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result
                .replace("<" + key + ">", value)
                .replace("{" + key + "}", value);
        }

        return result;
    }

    private static String stripMiniTags(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return MINI_TAG_PATTERN.matcher(input).replaceAll("");
    }
}