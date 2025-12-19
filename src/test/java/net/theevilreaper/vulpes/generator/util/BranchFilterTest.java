package net.theevilreaper.vulpes.generator.util;

import net.theevilreaper.vulpes.generator.domain.configuration.BranchFilterConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BranchFilterTest {

    private static final List<String> EXISTING_BRANCHES = List.of("master", "dev", "develop");

    @Disabled
    @Test
    void testBranchFiltering() {
        BranchFilterConfiguration filters = new BranchFilterConfiguration(EXISTING_BRANCHES);
        List<String> filteredBranches = BranchFilter.filterBranches(EXISTING_BRANCHES, filters);
        assertEquals(2, filteredBranches.size());
        List<String> expectedBranches = List.of("master", "develop");
        for (int i = 0; i < expectedBranches.size(); i++) {
            assertTrue(filteredBranches.contains(expectedBranches.get(i)));
        }
    }
}