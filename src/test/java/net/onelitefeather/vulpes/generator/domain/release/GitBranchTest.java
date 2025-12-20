package net.onelitefeather.vulpes.generator.domain.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitBranchTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void cleanup() {
        objectMapper = null;
    }

    @Test
    void testSerializeDeserialize() throws Exception {
        GitBranch branch = new GitBranch("main", true);

        String json = objectMapper.writeValueAsString(branch);
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"main\""));
        assertTrue(json.contains("\"protected\":true"));

        GitBranch deserialized = objectMapper.readValue(json, GitBranch.class);
        assertEquals(branch, deserialized);
    }
}
