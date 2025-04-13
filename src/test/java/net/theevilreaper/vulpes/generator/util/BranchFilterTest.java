package net.theevilreaper.vulpes.generator.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BranchFilterTest {

    private static final List<String> EXISTING_BRANCHES = List.of("master", "dev", "develop");

    @Test
    void testBranchFiltering() {
        List<String> filteredBranches = BranchFilter.filterBranches(EXISTING_BRANCHES, branch -> !branch.equalsIgnoreCase("dev"));
        assertEquals(2, filteredBranches.size());
        List<String> expectedBranches = List.of("master", "develop");
        for (int i = 0; i < expectedBranches.size(); i++) {
            assertTrue(filteredBranches.contains(expectedBranches.get(i)));
        }
    }
}