package net.theevilreaper.vulpes.generator.domain.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import jakarta.annotation.Nullable;
import net.theevilreaper.vulpes.generator.domain.release.GitBranch;
import net.theevilreaper.vulpes.generator.domain.release.GitTag;
import net.theevilreaper.vulpes.generator.domain.release.GitRelease;

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
     * Returns all branches for the given repository
     *
     * @param owner the owner of the repository
     * @param repo  the repository name
     * @return a list of branches
     */
    @Get("/repos/{owner}/{repo}/branches{?per_page,page}")
    HttpResponse<List<GitBranch>> branches(String owner, String repo, @PathVariable("per_page") Integer perPage, @Nullable Integer page);
}
