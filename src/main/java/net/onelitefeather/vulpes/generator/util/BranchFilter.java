package net.onelitefeather.vulpes.generator.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.List;

@ApiStatus.Internal
public final class BranchFilter {

    /**
     * Filters the branches and returns a list with the filtered branches.
     *
     * @param branches  The branches which should be filtered
     * @param predicate The predicate which should be used for the filtering
     * @return a list with the filtered branches
     */
    public static @NotNull List<String> filterBranches(@NotNull List<String> branches, @NotNull Predicate<String> predicate) {
        return branches.stream().filter(predicate).toList();
    }

    private BranchFilter() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
