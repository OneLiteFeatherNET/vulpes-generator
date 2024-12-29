package net.theevilreaper.vulpes.generator.controller

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import jakarta.inject.Inject
import net.theevilreaper.vulpes.generator.generation.GitProjectWorker
import net.theevilreaper.vulpes.generator.properties.GitlabProperties
import net.theevilreaper.vulpes.generator.registry.RegistryProvider
import net.theevilreaper.vulpes.generator.util.BASE_PACKAGE
import net.theevilreaper.vulpes.generator.util.BranchFilter
import net.theevilreaper.vulpes.generator.util.FileHelper
import net.theevilreaper.vulpes.generator.util.GRADLE_PROPERTIES
import net.theevilreaper.vulpes.generator.util.JAVA_MAIM_FOLDER
import net.theevilreaper.vulpes.generator.util.OUT_PUT_FOLDER
import net.theevilreaper.vulpes.generator.util.commitMail
import net.theevilreaper.vulpes.generator.util.commitMessage
import net.theevilreaper.vulpes.generator.util.commitName
import net.theevilreaper.vulpes.generator.util.gitlabCiFile
import net.theevilreaper.vulpes.generator.util.tempPrefix
import net.theevilreaper.vulpes.generator.util.version
import net.theevilreaper.vulpes.generator.util.zipFileName
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.outputStream
import kotlin.io.path.reader
import kotlin.io.path.writeText
import kotlin.io.path.writer

@Controller
class GeneratorHandler @Inject constructor(
    private val registryProvider: RegistryProvider,
    private val properties: GitlabProperties,
) {

    private val ignoredPrefix = "renovate/"

    @Get("/branches", produces = [MediaType.APPLICATION_JSON])
    private fun getBranches(
        @QueryValue(value = "full",) full: Boolean,
    ): HttpResponse<List<String>> {
        val refs = Git.lsRemoteRepository()
            .setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    properties.user,
                    properties.password
                )
            )
            .setHeads(true).setRemote(properties.remoteUrl).call().map { it.name }.toList()
        val filtered = BranchFilter.filterBranches(refs) { !it.contains(ignoredPrefix) }
        return if (full) {
            HttpResponse.ok(filtered).contentType(MediaType.APPLICATION_JSON)
        } else {
            val branches =
                BranchFilter.filterBranches(refs.map { it.substringAfter("refs/heads/") }) { !it.contains(ignoredPrefix) }
            HttpResponse.ok(branches).contentType(MediaType.APPLICATION_JSON)
        }
    }

    @Get("/generate", produces = [MediaType.APPLICATION_JSON])
    private fun generate(
        @QueryValue("branch") branch: String, @QueryValue("image") image: String?,
    ): HttpResponse<Any> {
        val tempPath = Files.createTempDirectory(tempPrefix)
        val output = tempPath.resolve(OUT_PUT_FOLDER)
        val tempGitlabCi = tempPath.resolve(gitlabCiFile)
        Files.copy(
            javaClass.classLoader.getResourceAsStream(gitlabCiFile),
            tempGitlabCi
        )
        Files.createDirectories(output)
        val gitlabCiFile = output.resolve(gitlabCiFile)

        val yaml = Yaml()
        tempGitlabCi.reader().use {
            val objects = yaml.load<MutableMap<String, Any>>(it)
            if (image != null) {
                objects["image"] = image
            }
            val variables = objects["variables"] as MutableMap<String, Any>
            variables["BRANCH"] = branch
            variables["GENERATION_URL"] = properties.deployUrl
            objects["variables"] = variables
            tempGitlabCi.writeText(yaml.dumpAsMap(objects))
        }

        val rawGit =
            Git.cloneRepository().setURI(properties.piplineUrl).setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    properties.user,
                    properties.password
                )
            ).setDirectory(output.toFile()).setCloneAllBranches(true)
        val git = rawGit.call()
        Files.copy(tempGitlabCi, gitlabCiFile, StandardCopyOption.REPLACE_EXISTING)
        val add = git.add()
        add.addFilepattern(net.theevilreaper.vulpes.generator.util.gitlabCiFile)
        add.call()
        val commit = git.commit()
        commit.setAuthor(commitName, commitMail)
        commit.message = commitMessage
        commit.setSign(false)
        commit.call()
        val push = git.push()
        push.setCredentialsProvider(
            UsernamePasswordCredentialsProvider(
                properties.user,
                properties.password
            )
        )
        push.call()
        return HttpResponse.accepted()
    }

    @Get("/download", produces = ["application/octet-stream"])
    private fun download(@QueryValue("branch", defaultValue = "master") branch: String): HttpResponse<Any> {
        println("MEEPO")
        val tempPath = Files.createTempDirectory(tempPrefix)
        val zipFile = tempPath.resolve("$OUT_PUT_FOLDER.zip")
        val output = tempPath.resolve(OUT_PUT_FOLDER)
        val javaPath = output.resolve(JAVA_MAIM_FOLDER)
        Files.createDirectories(output)
        val worker = GitProjectWorker(output.toFile(), properties.remoteUrl, branch, properties.user, properties.password)
        worker.cloneAndCheckout()
        val zipStream = javaClass.classLoader.getResourceAsStream(zipFileName)
        if (zipStream != null) {
            ZipInputStream(zipStream).use {
                generateSequence { it.nextEntry }
                    .forEach { entry ->
                        val path = output.resolve(entry.name)
                        if (entry.isDirectory) {
                            Files.createDirectories(path)
                        } else {
                            path.outputStream().use { output ->
                                it.copyTo(output)
                            }
                        }
                    }
            }
            val buildGradle = output.resolve(GRADLE_PROPERTIES)
            applyVulpesData(buildGradle)
            registryProvider.getRegistry().triggerAll(javaPath)
            Files.createFile(zipFile)
            FileHelper.zipFile(output, zipFile)
        }
        return HttpResponse.ok(zipFile.toFile())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .headers {
                it.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=generated.zip")
            } as HttpResponse<Any>
    }

    /**
     * Applies some vulpes relevant data to a file from gradle.
     * @param
     */
    private fun applyVulpesData(buildGradle: Path) {
        buildGradle.let {
            val properties = Properties()
            properties.load(it.reader())
            properties["vulpesGroupId"] = BASE_PACKAGE
            properties["vulpesBaseUrl"] = this.properties.dependencyUrl
            properties["vulpesVersion"] = version
            properties.store(it.writer(), "Generated Config")
        }
    }
}
