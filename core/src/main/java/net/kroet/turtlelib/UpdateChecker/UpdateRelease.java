package net.kroet.turtlelib.UpdateChecker;

/**
 * Newest matching release on one platform. {@code url} points to the release
 * page and may be null when the platform does not provide one.
 */
public record UpdateRelease(String version, String url) {
}
