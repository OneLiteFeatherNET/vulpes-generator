package net.theevilreaper.vulpes.generator.domain.client;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import net.theevilreaper.vulpes.generator.domain.release.GitTag;
import net.theevilreaper.vulpes.generator.domain.release.GitRelease;

import java.util.List;

@Client("github")
public interface GithubBuildClient {

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
}
