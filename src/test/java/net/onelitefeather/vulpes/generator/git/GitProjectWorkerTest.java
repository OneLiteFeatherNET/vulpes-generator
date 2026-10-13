package net.onelitefeather.vulpes.generator.git;

import net.onelitefeather.vulpes.generator.domain.configuration.GithubConfiguration;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitProjectWorkerTest {

    @TempDir
    Path tempDir;

    @Test
    void cloneBaseRepoChecksOutRequestedBranchInsteadOfRemoteHead() throws Exception {
        Path source = tempDir.resolve("source");
        createRepositoryWithDivergingBranches(source);

        GithubConfiguration configuration = new GithubConfiguration(
                "owner", "repo", "user", "token", source.toUri().toString()
        );
        GitProjectWorker worker = new GitProjectWorker(configuration);
        Path target = tempDir.resolve("target");

        try (Git git = worker.cloneBaseRepo(target, List.of("refs/heads/feature"))) {
            assertNotNull(git, "clone must succeed");
            assertEquals("feature", git.getRepository().getBranch(),
                    "the requested branch must be checked out");
            assertTrue(Files.exists(target.resolve("feature.txt")),
                    "the file that only exists on the requested branch must be checked out");
        }
    }

    private static void createRepositoryWithDivergingBranches(Path source) throws Exception {
        try (Git git = Git.init().setDirectory(source.toFile()).setInitialBranch("develop").call()) {
            commitFile(git, source, "develop.txt", "develop");
            git.branchCreate().setName("feature").call();
            git.checkout().setName("feature").call();
            commitFile(git, source, "feature.txt", "feature");
            git.checkout().setName("develop").call();
        }
    }

    private static void commitFile(Git git, Path source, String name, String content) throws Exception {
        Files.writeString(source.resolve(name), content, StandardCharsets.UTF_8);
        git.add().addFilepattern(name).call();
        git.commit().setMessage("add " + name)
                .setAuthor("test", "test@example.org")
                .setCommitter("test", "test@example.org")
                .call();
    }
}
