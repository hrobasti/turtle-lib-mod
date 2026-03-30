package com.github.hrobasti.turtlelib.VersionComparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionComparatorTest {

    @Test
    void releaseBeatsRc() {
        assertTrue(VersionComparator.isGreater("1.2.0", "1.2.0-rc1"));
    }

    @Test
    void rcBeatsBeta() {
        assertTrue(VersionComparator.isGreater("1.2.0-rc1", "1.2.0-beta4"));
    }

    @Test
    void betaNumberMatters() {
        assertTrue(VersionComparator.isGreater("1.2.0-beta10", "1.2.0-beta2"));
    }

    @Test
    void dotQualifierIsParsed() {
        assertTrue(VersionComparator.isGreater("1.2.0", "1.2.0.rc2"));
    }

    @Test
    void letterSuffixOnMainSegmentIsParsed() {
        assertTrue(VersionComparator.isGreater("1.2.0b", "1.2.0a"));
        assertTrue(VersionComparator.isGreater("1.2.0a", "1.2.0"));
        assertFalse(VersionComparator.isGreater("1.2.0", "1.2.0a"));
    }
}

