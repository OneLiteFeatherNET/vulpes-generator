package net.onelitefeather.vulpes.generator.release.base;

import org.eclipse.jgit.api.Git;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A local bare repository that stands in for {@code vulpes-base}, with one commit on {@code develop}. No network.
 *
 * @author TheMeinerLP
 */
final class LocalBaseOrigin {

    /**
     * The origin of the fixture.
     *
     * @param url  the URL to clone from
     * @param head the full SHA of the commit on {@code develop}
     */
    record Origin(String url, String head) {
    }

    private LocalBaseOrigin() {
    }

    static Origin create(Path dir) throws Exception {
        Path work = dir.resolve("source");
        String head;
        try (Git git = Git.init().setDirectory(work.toFile()).setInitialBranch("develop").call()) {
            Files.writeString(work.resolve("README.md"), "vulpes-base fixture", StandardCharsets.UTF_8);
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("initial commit")
                    .setAuthor("test", "test@example.org")
                    .setCommitter("test", "test@example.org")
                    .call();
            head = git.getRepository().resolve("HEAD").name();
        }
        Path origin = dir.resolve("origin.git");
        Git.cloneRepository()
                .setBare(true)
                .setURI(work.toUri().toString())
                .setDirectory(origin.toFile())
                .call()
                .close();
        return new Origin(origin.toUri().toString(), head);
    }
}
