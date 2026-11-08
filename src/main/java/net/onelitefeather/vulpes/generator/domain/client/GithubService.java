package net.onelitefeather.vulpes.generator.domain.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.GithubConfiguration;
import net.onelitefeather.vulpes.generator.domain.release.GitBranch;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;

import java.util.List;

@Singleton
public class GithubService {

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

    private String authorization() {
        String password = config.password();
        return password == null || password.isBlank() ? null : "Bearer " + password;
    }
}
