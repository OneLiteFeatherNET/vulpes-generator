package net.onelitefeather.vulpes.generator.domain.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import jakarta.annotation.Nullable;
import net.onelitefeather.vulpes.generator.domain.release.GitBranch;
import net.onelitefeather.vulpes.generator.domain.release.GitBranchHead;
import net.onelitefeather.vulpes.generator.domain.release.GitTag;
import net.onelitefeather.vulpes.generator.domain.release.GitRelease;

import java.util.List;

/**
 * The intention behind the {@link GithubClient} is to provide method
 * endpoints to fetch some specific
 * release data from a Github repository.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Client("github")
@Header(name = "User-Agent", value = "Vulpes-Generator")
public interface GithubClient {

    /**
     * Returns the latest release for the given repository
     *
     * @param owner the owner of the repository
     * @param repo  the repository name
     * @return the latest release as {@link GitRelease}
     */
    @Get("/repos/{owner}/{repo}/releases/latest")
    GitRelease latestRelease(String owner, String repo);

    /**
     * Returns all tags for the given repository
     *
     * @param owner the owner of the repository
     * @param repo  the repository name
     * @return a list of tags
     */
    @Get("/repos/{owner}/{repo}/tags")
    List<GitTag> tags(String owner, String repo);

    /**
     * Returns one branch of the given repository with the commit its head points to. The branch name may contain
     * slashes; the client encodes it as one path segment.
     *
     * @param owner         the owner of the repository
     * @param repo          the repository name
     * @param branch        the branch name, such as {@code feature/font}
     * @param authorization the Authorization header value, such as {@code Bearer <token>}; {@code null} sends none
     * @return the branch with its head commit
     */
    @Get("/repos/{owner}/{repo}/branches/{branch}")
    HttpResponse<GitBranchHead> branch(
            String owner,
            String repo,
            String branch,
            @Nullable @Header("Authorization") String authorization
    );

    /**
     * Returns all branches for the given repository
     *
     * @param owner   the owner of the repository
     * @param repo    the repository name
     * @param perPage the number of items per page
     * @param page    the page number
     * @param authorization the Authorization header value, such as {@code Bearer <token>}; {@code null} sends none
     * @return a list of branches
     */
    @Get("/repos/{owner}/{repo}/branches{?per_page,page}")
    HttpResponse<List<GitBranch>> branches(
            String owner,
            String repo,
            @Nullable @PathVariable("per_page") Integer perPage,
            @Nullable Integer page,
            @Nullable @Header("Authorization") String authorization
    );
}
