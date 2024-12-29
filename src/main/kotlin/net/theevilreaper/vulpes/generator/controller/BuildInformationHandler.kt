package net.theevilreaper.vulpes.generator.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import net.theevilreaper.vulpes.generator.properties.GitlabProperties
import org.gitlab4j.api.Constants
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.PackageFilter
import org.gitlab4j.api.models.Project

@Controller
class BuildInformationHandler @Inject constructor(
    gitLabConfig: GitlabProperties
) {

    private val gitLabApi: GitLabApi = GitLabApi(gitLabConfig.gitlabUrl, gitLabConfig.accessToken)

    @Get("/build/data", produces = [MediaType.APPLICATION_JSON])
    private fun getBuildInformation(): HttpResponse<BuildInformation> {
        val projectApi = gitLabApi.projectApi
        val projects: List<Project> = projectApi.getProjects("aves")

        if (projects.isEmpty()) {
            return HttpResponse.ok(BuildInformation(mapOf("error" to "Project not found")))
        }

        val avesProject = projects.first()
        val filter = PackageFilter().withOrderBy(Constants.PackageOrderBy.CREATED_AT)
            .withSortOder(Constants.SortOrder.DESC)

        val latestReleases = gitLabApi.packagesApi.getPackages(avesProject.id, filter, 1)

        if (!latestReleases.hasNext()) {
            return HttpResponse.ok(BuildInformation(mapOf("error" to "No releases found")))
        }

        val release = latestReleases.first().first()
        val data: MutableMap<String, String> = mutableMapOf()
        data["version"] = release.version
        data["created"] = release.createdAt.toString()
        return HttpResponse.ok(BuildInformation(data))
    }

    data class BuildInformation(
        val data: Map<String, String>
    )
}
