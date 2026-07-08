package com.github.hrobasti.turtlelib.LangLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Loader-neutral locale loader/synchronizer for mod resources.
 *
 * <p>Expected bundled files: {@code /lang/<locale>.properties}, e.g. {@code lang/en_US.properties}.</p>
 */
public final class LangLoader {
    public static final String DEFAULT_LOCALE = "en_US";
    private static final String RESOURCE_PREFIX = "lang/";
    private static final String EXTENSION = ".properties";
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[a-z]{2}_[A-Z]{2}$");

    private LangLoader() {
    }

    public static List<String> getBundledLocales(Class<?> resourceAnchor) {
        Set<String> locales = new TreeSet<>();
        locales.addAll(scanBundledLocales(resourceAnchor));
        if (locales.isEmpty()) {
            locales.add(DEFAULT_LOCALE);
        }
        return List.copyOf(locales);
    }

    public static List<String> getAvailableLocales(Class<?> resourceAnchor, Path langDir) {
        Set<String> locales = new TreeSet<>();
        locales.addAll(scanBundledLocales(resourceAnchor));
        locales.addAll(scanDataLocales(langDir));
        if (locales.isEmpty()) {
            locales.add(DEFAULT_LOCALE);
        }
        return List.copyOf(locales);
    }

    public static void ensureBundledLocales(Class<?> resourceAnchor, Path langDir) {
        for (String locale : getBundledLocales(resourceAnchor)) {
            ensureLocaleFile(resourceAnchor, langDir, locale);
        }
    }

    public static List<String> syncLocale(Class<?> resourceAnchor, Path langDir, String locale) {
        String normalized = normalize(locale);
        if (normalized == null) {
            return Collections.emptyList();
        }

        Path targetFile = ensureLocaleFile(resourceAnchor, langDir, normalized);
        Properties defaults = loadDefaults(resourceAnchor, normalized);
        if (defaults == null) {
            return Collections.emptyList();
        }

        Properties current = loadProperties(targetFile);
        List<String> addedKeys = new ArrayList<>();
        boolean changed = false;
        for (String key : defaults.stringPropertyNames()) {
            if (!current.containsKey(key)) {
                current.setProperty(key, defaults.getProperty(key));
                addedKeys.add(key);
                changed = true;
            }
        }

        if (changed) {
            saveProperties(targetFile, current);
        }

        return List.copyOf(addedKeys);
    }

    public static Properties loadLocale(Class<?> resourceAnchor, Path langDir, String locale) {
        ensureBundledLocales(resourceAnchor, langDir);

        String normalized = normalize(locale);
        if (normalized == null) {
            normalized = DEFAULT_LOCALE;
        }

        syncLocale(resourceAnchor, langDir, normalized);
        Path localeFile = ensureLocaleFile(resourceAnchor, langDir, normalized);

        Properties current = loadProperties(localeFile);
        Properties defaults = loadDefaults(resourceAnchor, DEFAULT_LOCALE);
        if (defaults == null) {
            return current;
        }

        for (String key : defaults.stringPropertyNames()) {
            current.putIfAbsent(key, defaults.getProperty(key));
        }
        return current;
    }

    public static Properties loadLocaleResource(Class<?> resourceAnchor, String locale) {
        String normalized = normalize(locale);
        if (normalized == null) {
            normalized = DEFAULT_LOCALE;
        }

        try (InputStream in = resourceAnchor.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + normalized + EXTENSION)) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static Path ensureLocaleFile(Class<?> resourceAnchor, Path langDir, String locale) {
        try {
            Files.createDirectories(langDir);
        } catch (IOException ignored) {
            // best effort, follow-up operations will fail gracefully
        }

        Path file = langDir.resolve(locale + EXTENSION);
        if (Files.exists(file)) {
            return file;
        }

        Properties bundled = loadLocaleResource(resourceAnchor, locale);
        if (bundled != null) {
            saveProperties(file, bundled);
            return file;
        }

        try {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        } catch (IOException ignored) {
            // ignored intentionally
        }

        return file;
    }

    private static Properties loadDefaults(Class<?> resourceAnchor, String locale) {
        Properties defaults = loadLocaleResource(resourceAnchor, locale);
        if (defaults == null && !DEFAULT_LOCALE.equals(locale)) {
            defaults = loadLocaleResource(resourceAnchor, DEFAULT_LOCALE);
        }
        return defaults;
    }

    private static Set<String> scanBundledLocales(Class<?> resourceAnchor) {
        Set<String> locales = new TreeSet<>();
        try {
            URL dirUrl = resourceAnchor.getClassLoader().getResource(RESOURCE_PREFIX);
            if (dirUrl == null) {
                return withDefault(locales);
            }

            String protocol = dirUrl.getProtocol();
            if ("jar".equals(protocol)) {
                scanJarLocales(dirUrl, locales);
            } else if ("file".equals(protocol)) {
                scanFileLocales(Paths.get(dirUrl.toURI()), locales);
            }
        } catch (Exception ignored) {
            return withDefault(locales);
        }

        return withDefault(locales);
    }

    private static void scanJarLocales(URL resourceUrl, Set<String> target) throws Exception {
        JarURLConnection connection = (JarURLConnection) resourceUrl.openConnection();
        URI jarUri = connection.getJarFileURL().toURI();
        URI fileSystemUri = URI.create("jar:" + jarUri);

        try (FileSystem fs = newFileSystemIfNeeded(fileSystemUri)) {
            Path langPath = fs.getPath("/" + RESOURCE_PREFIX);
            scanFileLocales(langPath, target);
        }
    }

    private static FileSystem newFileSystemIfNeeded(URI jarUri) throws IOException {
        try {
            return FileSystems.getFileSystem(jarUri);
        } catch (Exception ignored) {
            return FileSystems.newFileSystem(jarUri, Collections.emptyMap());
        }
    }

    private static void scanFileLocales(Path langPath, Set<String> target) {
        if (langPath == null || !Files.exists(langPath) || !Files.isDirectory(langPath)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(langPath, "*" + EXTENSION)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                String locale = name.substring(0, name.length() - EXTENSION.length());
                if (isLocaleIdentifier(locale)) {
                    target.add(locale);
                }
            }
        } catch (IOException ignored) {
            // ignored intentionally
        }
    }

    private static Set<String> scanDataLocales(Path langDir) {
        if (langDir == null || !Files.exists(langDir) || !Files.isDirectory(langDir)) {
            return Collections.emptySet();
        }

        Set<String> locales = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(langDir, "*" + EXTENSION)) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                String locale = name.substring(0, name.length() - EXTENSION.length());
                if (isLocaleIdentifier(locale)) {
                    locales.add(locale);
                }
            }
        } catch (IOException ignored) {
            return Collections.emptySet();
        }
        return locales;
    }

    private static boolean isLocaleIdentifier(String candidate) {
        return candidate != null && LOCALE_PATTERN.matcher(candidate).matches();
    }

    private static Properties loadProperties(Path file) {
        Properties properties = new Properties();
        if (file == null || !Files.exists(file)) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) {
            return new Properties();
        }
        return properties;
    }

    private static void saveProperties(Path file, Properties properties) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Generated by TurtleLib LangLoader");
            }
        } catch (IOException ignored) {
            // ignored intentionally
        }
    }

    private static String normalize(String locale) {
        if (locale == null) {
            return null;
        }

        String trimmed = locale.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.endsWith(EXTENSION)) {
            trimmed = trimmed.substring(0, trimmed.length() - EXTENSION.length());
        }

        if (trimmed.contains("-")) {
            String[] tokens = trimmed.split("-");
            if (tokens.length == 2) {
                trimmed = tokens[0].toLowerCase(Locale.ROOT) + "_" + tokens[1].toUpperCase(Locale.ROOT);
            }
        }

        return trimmed;
    }

    private static Set<String> withDefault(Set<String> locales) {
        if (!locales.contains(DEFAULT_LOCALE)) {
            locales.add(DEFAULT_LOCALE);
        }
        return locales;
    }
}

