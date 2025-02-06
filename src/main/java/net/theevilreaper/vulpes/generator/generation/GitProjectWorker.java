package net.theevilreaper.vulpes.generator.generation;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GitProjectWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitProjectWorker.class);

    private final File basePath;
    private final String remoteUrl;
    private final String reference;
    private final String gitUsername;
    private final String gitPassword;

    public GitProjectWorker(File basePath, String remoteUrl, String reference, String gitUsername, String gitPassword) {
        this.basePath = basePath;
        this.remoteUrl = remoteUrl;
        this.reference = reference;
        this.gitUsername = gitUsername;
        this.gitPassword = gitPassword;
    }

    public void cloneAndCheckout() {
        try {
            var rawGit =
                    Git.cloneRepository().setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(
                                    gitUsername,
                                    gitPassword
                            )
                    ).setURI(remoteUrl).setDirectory(basePath).setCloneAllBranches(true);
            var git = rawGit.call();
            LOGGER.info("Git reference is $reference");
            var objectId = git.getRepository().resolve(reference);
            if (objectId != null) {
                LOGGER.info("Name from the git object is ${objectId.name}");
                git.checkout().setName(objectId.name()).call();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
