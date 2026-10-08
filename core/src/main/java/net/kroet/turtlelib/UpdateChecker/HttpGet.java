package net.kroet.turtlelib.UpdateChecker;

import java.io.IOException;

/**
 * Fetches the body of a GET request; replaced by a fake in tests.
 */
@FunctionalInterface
interface HttpGet {
    String get(String url, String userAgent) throws IOException, InterruptedException;
}
