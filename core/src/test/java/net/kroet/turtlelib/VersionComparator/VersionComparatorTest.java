package net.kroet.turtlelib.VersionComparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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

    @Test
    void ignoresLeadingVAndBuildMetadata() {
        assertEquals(0, VersionComparator.compare("v2.0.0", "2.0.0"));
        assertEquals(0, VersionComparator.compare("2.0.0+26.3", "2.0.0"));
        assertTrue(VersionComparator.isGreater("v2.1.0", "2.0.0"));
    }

    @Test
    void comparesPrereleaseNumbersAfterSeparators() {
        assertTrue(VersionComparator.isGreater("2.0.0-beta.2", "2.0.0-beta.1"));
        assertTrue(VersionComparator.isGreater("2.0.0-beta-10", "2.0.0-beta-9"));
        assertTrue(VersionComparator.isGreater("2.0.0", "2.0.0-rc.1"));
    }
}
