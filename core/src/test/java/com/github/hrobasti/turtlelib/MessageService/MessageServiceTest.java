package com.github.hrobasti.turtlelib.MessageService;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void loadAndFormatUsesPrefixAndReplacements() throws IOException {
        Path langDir = tempDir.resolve("lang");
        Files.createDirectories(langDir);
        Files.writeString(
            langDir.resolve("en_US.properties"),
            "ui.prefix=[<prefix_label>]\n" +
                "hello=<prefix> Hello <name>"
        );

        MessageService service = new MessageService(MessageServiceTest.class, langDir, "[<prefix_label>]", "TurtleLib");
        service.load("en_US");

        String message = service.format("hello", Map.of("name", "Alex"));
        assertEquals("[TurtleLib] Hello Alex", message);
        assertEquals("[TurtleLib] Hello Alex", service.plain("hello", Map.of("name", "Alex")));
    }

    @Test
    void reloadReadsLanguageFromProperties() throws IOException {
        Path langDir = tempDir.resolve("lang");
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("de_DE.properties"), "hello=Hallo");

        MessageService service = new MessageService(MessageServiceTest.class, langDir, "[<prefix_label>]", "TL");
        Properties config = new Properties();
        config.setProperty("language", "de_DE");

        service.reload(config);

        assertEquals("de_DE", service.getLanguage());
        assertEquals("Hallo", service.component("hello"));
    }
}