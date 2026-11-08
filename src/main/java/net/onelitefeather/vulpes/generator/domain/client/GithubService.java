package net.onelitefeather.vulpes.generator.domain.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.GithubConfiguration;
import net.onelitefeather.vulpes.generator.domain.release.GitBranch;
import net.onelitefeather.vulpes.generator.domain.release.GitBranchHead;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public class GithubService {

    private static final Logger LOG = LoggerFactory.getLogger(GithubService.class);

    private final GithubClient client;
    private final GithubConfiguration config;

    /**
     * Creates a new instance of the {@link GithubService}
     *
     * @param client the {@link GithubClient} to use for retrieving the branches.
     * @param config the {@link GithubConfiguration} to use for retrieving the
     *               repository information.
     */
    @Inject
    public GithubService(GithubClient client, GithubConfiguration config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Returns a list of all branches in the repository. The configured {@code github.password} is sent as the
     * bearer token when it is set, because the unauthenticated rate limit is small.
     *
     * @return a list of all branches
     * @throws ReleaseRejectedException with {@code BASE_UNAVAILABLE} when GitHub does not answer with 200 or cannot
     *                                  be reached; an empty list would refuse every branch
     */
    public List<String> getBranches() {
        HttpResponse<List<GitBranch>> response;
        try {
            response = client.branches(config.owner(), config.repo(), 100, 1, authorization());
        } catch (HttpClientException e) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                    "branches of " + config.owner() + "/" + config.repo() + " cannot be read from GitHub");
        }
        if (response.code() != 200) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                    "branches of " + config.owner() + "/" + config.repo() + " answered " + response.code());
        }

        List<GitBranch> branches = response.body();
        return branches.stream()
                .map(GitBranch::name)
                .toList();
    }

    /**
     * Returns the full SHA of the head commit of a branch, asked from the GitHub REST API. No clone is made, so a
     * commit that is already known costs one request. The configured {@code github.password} is sent as the bearer
     * token when it is set.
     *
     * @param branch the branch name, such as {@code develop} or {@code feature/font}
     * @return the full 40-character lower-case SHA-1 of the head commit
     * @throws ReleaseRejectedException with {@code BASE_UNAVAILABLE} when GitHub does not answer with 200 for the
     *                                  branch or cannot be reached; the cause is logged as a WARN
     */
    public String resolveCommit(String branch) {
        HttpResponse<GitBranchHead> response;
        try {
            response = client.branch(config.owner(), config.repo(), branch, authorization());
        } catch (HttpClientException e) {
            throw commitUnavailable(branch, e);
        }
        if (response.code() != 200) {
            throw commitUnavailable(branch, "answered " + response.code());
        }
        return response.body().commit().sha();
    }

    private ReleaseRejectedException commitUnavailable(String branch, HttpClientException cause) {
        LOG.warn("head of branch {} of {}/{} cannot be read from GitHub: {}", branch, config.owner(), config.repo(),
                causeChain(cause));
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                "branch " + branch + " of " + config.owner() + "/" + config.repo() + " cannot be resolved");
    }

    private ReleaseRejectedException commitUnavailable(String branch, String detail) {
        LOG.warn("head of branch {} of {}/{} cannot be read from GitHub: GitHub {}", branch, config.owner(),
                config.repo(), detail);
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                "branch " + branch + " of " + config.owner() + "/" + config.repo() + " cannot be resolved");
    }

    private static String causeChain(Throwable e) {
        StringBuilder chain = new StringBuilder();
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (!chain.isEmpty()) {
                chain.append(" <- ");
            }
            chain.append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
        }
        return chain.toString();
    }

    private String authorization() {
        String password = config.password();
        return password == null || password.isBlank() ? null : "Bearer " + password;
    }
}
