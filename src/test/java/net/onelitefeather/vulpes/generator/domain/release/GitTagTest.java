package net.onelitefeather.vulpes.generator.domain.release;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitTagTest {

    @Test
    void testEmptyGitTag() {
        GitTag tag = new GitTag(null);
        assertNotNull(tag);
        assertEquals("Unknown", tag.name());
    }

    @Test
    void testSimpleGitTag() {
        GitTag tag = new GitTag("1.0.0");
        assertNotNull(tag);
        assertEquals("1.0.0", tag.name());
    }
}
