package net.kroet.turtlelib.UpdateChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpdateCheckerTest {
    private static final String MODRINTH = """
            [
              {"id":"v3","project_id":"p1","version_number":"2.2.0-alpha.1","version_type":"alpha","loaders":["fabric"],"game_versions":["26.3"]},
              {"id":"v2","project_id":"p1","version_number":"2.1.0","version_type":"release","loaders":["fabric"],"game_versions":["26.3"]},
              {"id":"v1","project_id":"p1","version_number":"2.0.0","version_type":"release","loaders":["fabric"],"game_versions":["26.3"]},
              {"id":"vn","project_id":"p1","version_number":"9.0.0","version_type":"release","loaders":["neoforge"],"game_versions":["26.3"]}
            ]
            """;
    private static final String CURSEFORGE = """
            {"id":1487139,"files":[
              {"url":"https://cf/files/3","name":"PermaClicker_2.3.0-beta.1_MC-26-3_FABRIC_0-19-5.jar","type":"beta","versions":["Client","Fabric","26.3"]},
              {"url":"https://cf/files/2","name":"PermaClicker_2.2.0_MC-26-3_FABRIC_0-19-5.jar","type":"release","versions":["Client","Fabric","26.3"]},
              {"url":"https://cf/files/n","name":"PermaClicker_9.0.0_MC-26-3_NEOFORGE_26-3-0-1-beta.jar","type":"release","versions":["NeoForge","26.3"]},
              {"url":"https://cf/files/o","name":"PermaClicker_8.0.0_MC-26-2_FABRIC_0-19-3.jar","type":"release","versions":["Fabric","26.2"]}
            ]}
            """;

    private final List<String> requested = new ArrayList<>();

    private UpdateChecker checker(String modrinthId, String curseforgeId, UpdateReleaseChannel channel, HttpGet http) {
        return new UpdateChecker("Test/1.0.0", modrinthId, curseforgeId, channel, "26.3", UpdateLoader.FABRIC, (url, agent) -> {
            requested.add(url);
            return http.get(url, agent);
        });
    }

    private static HttpGet serving(String modrinth, String curseforge) {
        return (url, agent) -> {
            if (url.startsWith("https://api.modrinth.com/")) {
                if (modrinth == null) {
                    throw new IOException("HTTP 503");
                }
                return modrinth;
            }
            if (curseforge == null) {
                throw new IOException("HTTP 202");
            }
            return curseforge;
        };
    }

    @Test
    void picksNewestMatchingReleasePerPlatformAndOverall() {
        UpdateResult result = checker("permaclicker-mod", "1487139", UpdateReleaseChannel.STABLE, serving(MODRINTH, CURSEFORGE))
                .check("2.0.0");

        assertEquals(new UpdateRelease("2.1.0", "https://modrinth.com/project/p1/version/v2"), result.releases().get(UpdatePlatform.MODRINTH));
        assertEquals(new UpdateRelease("2.2.0", "https://cf/files/2"), result.releases().get(UpdatePlatform.CURSEFORGE));
        assertEquals("2.2.0", result.latestVersion());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void filtersModrinthOnTheServerByLoaderAndMinecraftVersion() {
        checker("permaclicker-mod", null, UpdateReleaseChannel.BETA, serving(MODRINTH, CURSEFORGE)).check("2.0.0");

        assertEquals(List.of("https://api.modrinth.com/v2/project/permaclicker-mod/version?include_changelog=false"
                + "&loaders=%5B%22fabric%22%5D&game_versions=%5B%2226.3%22%5D"), requested);
    }

    @Test
    void queriesCurseForgeByProjectId() {
        checker(null, "1487139", UpdateReleaseChannel.BETA, serving(MODRINTH, CURSEFORGE)).check("2.0.0");

        assertEquals(List.of("https://api.cfwidget.com/1487139"), requested);
    }

    @Test
    void channelControlsWhichReleaseTypesCount() {
        UpdateResult beta = checker("m", "1", UpdateReleaseChannel.BETA, serving(MODRINTH, CURSEFORGE)).check("2.0.0");
        assertEquals("2.1.0", beta.releases().get(UpdatePlatform.MODRINTH).version());
        assertEquals("2.3.0-beta.1", beta.releases().get(UpdatePlatform.CURSEFORGE).version());

        UpdateResult alpha = checker("m", "1", UpdateReleaseChannel.ALPHA, serving(MODRINTH, CURSEFORGE)).check("2.0.0");
        assertEquals("2.2.0-alpha.1", alpha.releases().get(UpdatePlatform.MODRINTH).version());
    }

    @Test
    void oneFailingPlatformDoesNotHideTheOther() {
        UpdateResult result = checker("m", "1", UpdateReleaseChannel.STABLE, serving(null, CURSEFORGE)).check("2.0.0");

        assertEquals("2.2.0", result.latestVersion());
        assertFalse(result.releases().containsKey(UpdatePlatform.MODRINTH));
        assertEquals("IOException: HTTP 503", result.errors().get(UpdatePlatform.MODRINTH));
    }

    @Test
    void bothPlatformsFailingYieldsNoUpdateAndTwoErrors() {
        UpdateResult result = checker("m", "1", UpdateReleaseChannel.STABLE, serving(null, null)).check("2.0.0");

        assertFalse(result.hasUpdate());
        assertTrue(result.releases().isEmpty());
        assertEquals(2, result.errors().size());
    }

    @Test
    void malformedOrEmptyAnswersCountAsNoMatchingRelease() {
        UpdateResult result = checker("m", "1", UpdateReleaseChannel.STABLE, serving("{}", "[]")).check("2.0.0");

        assertEquals(Map.of(UpdatePlatform.MODRINTH, "no matching release", UpdatePlatform.CURSEFORGE, "no matching release"),
                result.errors());
    }

    @Test
    void noUpdateWhenCurrentVersionIsNewest() {
        UpdateResult result = checker("m", "1", UpdateReleaseChannel.STABLE, serving(MODRINTH, CURSEFORGE)).check("3.0.0");

        assertFalse(result.hasUpdate());
        assertNull(result.latestVersion());
        assertEquals(2, result.releases().size());
    }

    @Test
    void missingProjectIdsSendNoRequests() {
        UpdateResult result = checker(" ", null, UpdateReleaseChannel.BETA, serving(MODRINTH, CURSEFORGE)).check("2.0.0");

        assertTrue(requested.isEmpty());
        assertTrue(result.releases().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void readsVersionFromFileName() {
        assertEquals("2.0.0", UpdateChecker.versionFromFileName("PermaClicker_2.0.0_MC-26-3_FABRIC_0-19-5.jar"));
        assertEquals("2.0.0-beta.1", UpdateChecker.versionFromFileName("TurtleLib_2.0.0-beta.1_MC-26-3_NEOFORGE_26-3-0-1-beta.jar"));
        assertEquals("1.4.0", UpdateChecker.versionFromFileName("PermaClicker 1.4.0"));
        assertNull(UpdateChecker.versionFromFileName("PermaClicker_MC-26-3.jar"));
        assertNull(UpdateChecker.versionFromFileName(null));
    }
}
