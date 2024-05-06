package net.theevilreaper.vulpes.generator.controller

import net.theevilreaper.vulpes.generator.properties.GitlabProperties
import org.gitlab4j.api.Constants
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.PackageFilter
import org.gitlab4j.api.models.Project
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"], maxAge = 4800, allowCredentials = "false")
@RestController
class BuildInformationHandler(
    private val gitLabConfig: GitlabProperties
) {

    private val gitLabApi: GitLabApi = GitLabApi(gitLabConfig.gitlabUrl, gitLabConfig.accessToken)

    @GetMapping("/build/data", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun getBuildInformation(): ResponseEntity<BuildInformation> {
        val projectApi = gitLabApi.projectApi
        val projects: List<Project> = projectApi.getProjects("aves")

        if (projects.isEmpty()) {
            return ResponseEntity.ok(BuildInformation(mapOf("error" to "Project not found")))
        }

        val avesProject = projects.first()
        val filter = PackageFilter().withOrderBy(Constants.PackageOrderBy.CREATED_AT)
            .withSortOder(Constants.SortOrder.DESC)

        val latestReleases = gitLabApi.packagesApi.getPackages(avesProject.id, filter, 1)

        if (!latestReleases.hasNext()) {
            return ResponseEntity.ok(BuildInformation(mapOf("error" to "No releases found")))
        }

        val release = latestReleases.first().first()
        val data: MutableMap<String, String> = mutableMapOf()
        data["version"] = release.version
        data["created"] = release.createdAt.toString()
        return ResponseEntity.ok(BuildInformation(data))
    }

    data class BuildInformation(
        val data: Map<String, String>
    )
}
