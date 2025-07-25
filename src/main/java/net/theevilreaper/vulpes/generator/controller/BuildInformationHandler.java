package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.domain.build.BuildInformationDTO;
import net.theevilreaper.vulpes.generator.properties.GitlabProperties;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Package;
import org.gitlab4j.api.models.PackageFilter;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.models.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller("/build")
public class BuildInformationHandler {

    private final GitLabApi gitLabApi;
    private final PackageFilter standardFilter;

    @Inject
    public BuildInformationHandler(@NotNull GitlabProperties gitlabProperties) {
        this.gitLabApi = new GitLabApi(gitlabProperties.gitlabUrl(), gitlabProperties.accessToken());
        standardFilter = new PackageFilter()
                .withOrderBy(Constants.PackageOrderBy.CREATED_AT)
                .withSortOder(Constants.SortOrder.DESC);
    }

    @Operation(
            summary = "Get build information",
            description = "Returns the latest build information including version and creation date.",
            tags = {"build"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "Build information retrieved successfully.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BuildInformationDTO.class)
            )
    )
    @Get(value = "/data", produces = MediaType.APPLICATION_JSON)
    public @NotNull HttpResponse<BuildInformationDTO> getBuildInformation() throws GitLabApiException {
        ProjectApi projectApi = gitLabApi.getProjectApi();
        List<Project> projects = projectApi.getProjects("aves");

        Project avesProject = projects.getFirst();
        Pager<Package> latestReleases = gitLabApi.getPackagesApi().getPackages(avesProject.getId(), standardFilter, 1);

        if (!latestReleases.hasNext()) {
            return HttpResponse.ok(new BuildInformationDTO(Map.of("error", "No releases found")));
        }
        Package release = latestReleases.first().getFirst();
        Map<String, String> data = new HashMap<>();
        data.put("version", release.getVersion());
        data.put("created", release.getCreatedAt().toString());
        return HttpResponse.ok(new BuildInformationDTO(data));
    }
}
