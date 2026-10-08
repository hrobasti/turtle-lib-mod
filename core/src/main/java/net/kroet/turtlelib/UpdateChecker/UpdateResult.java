package net.kroet.turtlelib.UpdateChecker;

import java.util.Map;

/**
 * Outcome of {@link UpdateChecker#check(String)}. {@code releases} holds only
 * platforms that answered with a matching release; {@code errors} holds a short
 * reason for every platform that failed. Neither map contains null values.
 */
public record UpdateResult(
        String currentVersion,
        String latestVersion,
        Map<UpdatePlatform, UpdateRelease> releases,
        Map<UpdatePlatform, String> errors) {
    public UpdateResult {
        releases = Map.copyOf(releases);
        errors = Map.copyOf(errors);
    }

    public boolean hasUpdate() {
        return latestVersion != null && !latestVersion.isBlank();
    }
}
