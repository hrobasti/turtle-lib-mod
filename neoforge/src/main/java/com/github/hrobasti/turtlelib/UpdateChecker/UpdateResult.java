package com.github.hrobasti.turtlelib.UpdateChecker;

import java.util.Map;

public record UpdateResult(String currentVersion, String latestVersion, Map<UpdatePlatform, String> providerVersions) {
    public boolean hasUpdate() {
        return latestVersion != null && !latestVersion.isBlank();
    }
}

