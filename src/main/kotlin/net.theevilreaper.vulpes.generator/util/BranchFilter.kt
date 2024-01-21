package net.theevilreaper.vulpes.generator.util

/**
 * The class contains some utility methods for the branch filtering.
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
object BranchFilter {

    /**
     * Filters the branches and returns a list with the filtered branches.
     * @param branches The branches which should be filtered
     * @param predicate The predicate which should be used for the filtering
     * @return A list with the filtered branches
     */
    inline fun filterBranches(branches: List<String>, crossinline predicate: (String) -> Boolean): List<String> {
        if (branches.isEmpty()) return emptyList()
        return branches.filter { predicate(it) }.toList()
    }
}
