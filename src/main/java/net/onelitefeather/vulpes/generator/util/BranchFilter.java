package net.onelitefeather.vulpes.generator.util;

import net.onelitefeather.vulpes.generator.domain.configuration.BranchFilterConfiguration;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public final class BranchFilter {

    /**
     * Filters the branches and returns a list with the filtered branches.
     *
     * @param branches            the branches which should be filtered
     * @param filterConfiguration the filter configuration to use
     * @return a list with the filtered branches
     */
    public static List<String> filterBranches(List<String> branches, BranchFilterConfiguration filterConfiguration) {
        return branches.stream().filter(entry ->
                        filterConfiguration.exclude().stream()
                                .noneMatch(entry::startsWith)
                )
                .toList();
    }

    private BranchFilter() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
