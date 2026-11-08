package net.onelitefeather.vulpes.generator.domain.release;

import io.micronaut.serde.annotation.Serdeable;

/**
 * A branch of a repository with the commit its head points to, as the branch endpoint of the GitHub REST API returns
 * it.
 *
 * @param name   the name of the branch
 * @param commit the commit the head of the branch points to
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record GitBranchHead(
        String name,
        Commit commit
) {

    /**
     * The commit a branch head points to.
     *
     * @param sha the full 40-character lower-case SHA-1 of the commit
     */
    @Serdeable
    public record Commit(String sha) {
    }
}
