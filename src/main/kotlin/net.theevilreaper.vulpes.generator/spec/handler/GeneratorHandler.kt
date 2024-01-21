package net.theevilreaper.vulpes.generator.spec.handler

import com.fasterxml.jackson.databind.ObjectMapper
import net.theevilreaper.vulpes.generator.generation.GeneratorRegistry
import net.theevilreaper.vulpes.generator.generation.GitProjectWorker
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.outputStream
import kotlin.io.path.reader
import kotlin.io.path.writeText
import kotlin.io.path.writer

@CrossOrigin(origins = ["*"], maxAge = 4800, allowCredentials = "false")
@RestController
class GeneratorHandler(
    val registry: GeneratorRegistry,
    val objectMapper: ObjectMapper,
    /*val gitLabApi: GitLabApi,
    val gitlabProperties: GitlabProperties,*/
) {

    @Value(value = "\${vulpes.remoteUrl}")
    private lateinit var remoteUrl: String

    @Value(value = "\${vulpes.piplineUrl}")
    private lateinit var pipelineUrl: String

    @Value(value = "\${vulpes.deployUrl}")
    private lateinit var deployUrl: String

    @Value(value = "\${vulpes.dependencyUrl}")
    private lateinit var dependencyUrl: String

    @Value(value = "\${vulpes.git.username}")
    private lateinit var gitUsername: String

    @Value(value = "\${vulpes.git.password}")
    private lateinit var gitPassword: String

    private val ignoredPrefix = "renovate/"

    @GetMapping("/branches", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun getBranches(
        @RequestParam(
            name = "full",
            required = false
        ) full: Boolean,
    ): ResponseEntity<List<String>> {
        val refs = Git.lsRemoteRepository()
            .setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    gitUsername,
                    gitPassword
                )
            )
            .setHeads(true).setRemote(remoteUrl).call().map { it.name }.toList()
        val filtered = BranchFilter.filterBranches(refs) { !it.contains(ignoredPrefix) }
        return if (full) {
            ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(filtered)
        } else {
            val branches =
                BranchFilter.filterBranches(refs.map { it.substringAfter("refs/heads/") }) { !it.contains(ignoredPrefix) }
            ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(branches)
        }
    }

    @GetMapping("/generate", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun generate(
        @RequestParam("branch") branch: String, @RequestParam("image", required = false) image: String?,
    ): ResponseEntity<Any> {
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
            variables["GENERATION_URL"] = deployUrl
            objects["variables"] = variables
            tempGitlabCi.writeText(yaml.dumpAsMap(objects))
        }

        val rawGit =
            Git.cloneRepository().setURI(pipelineUrl).setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    gitUsername,
                    gitPassword
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
                gitUsername,
                gitPassword
            )
        )
        push.call()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/download", produces = ["application/octet-stream"])
    private fun download(@RequestParam("branch") branch: String): ResponseEntity<Any> {
        val tempPath = Files.createTempDirectory(tempPrefix)
        val zipFile = tempPath.resolve("$OUT_PUT_FOLDER.zip")
        val output = tempPath.resolve(OUT_PUT_FOLDER)
        val javaPath = output.resolve(JAVA_MAIM_FOLDER)
        Files.createDirectories(output)
        val worker = GitProjectWorker(output.toFile(), remoteUrl, branch, gitUsername, gitPassword)
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
            buildGradle.let {
                val properties = Properties()
                properties.load(it.reader())
                properties["vulpesGroupId"] = BASE_PACKAGE
                properties["vulpesBaseUrl"] = dependencyUrl
                properties["vulpesVersion"] = version
                properties.store(it.writer(), "Generated Config")
            }
            registry.getGenerators().values.forEach {
                if (it.getType() == GeneratorType.DART) return@forEach
                it.generate(javaPath)
            }
            Files.createFile(zipFile)
            FileHelper.zipFile(output, zipFile)
        }
        registry.cleanup()
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=generated.zip")
            .body(FileSystemResource(zipFile.toFile()))
    }
}
