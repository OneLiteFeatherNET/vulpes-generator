package net.theevilreaper.vulpes.generator.spec.handler

import com.fasterxml.jackson.databind.ObjectMapper
import net.theevilreaper.vulpes.generator.generation.GeneratorRegistry
import net.theevilreaper.vulpes.generator.generation.GeneratorType
import net.theevilreaper.vulpes.generator.generation.GitProjectWorker
import net.theevilreaper.vulpes.generator.util.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

@CrossOrigin(origins = ["*"], maxAge = 4800, allowCredentials = "false")
@RestController
class GeneratorHandler(
    val registry: GeneratorRegistry,
    val objectMapper: ObjectMapper
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

    @RequestMapping("/branches", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBranches(@RequestParam(name = "full", required = false) full: Boolean): ResponseEntity<List<String>> {
        val refs = Git.lsRemoteRepository().setHeads(true).setRemote(remoteUrl).call().map { it.name }.toList()
        return if (full) {
            ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(refs)
        } else {
            val branches = refs.map { it.substringAfter("refs/heads/") }
            ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(branches)
        }
    }

    @RequestMapping("/generate", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun generate(
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

    @RequestMapping("/download", method = [RequestMethod.GET], produces = ["application/octet-stream"])
    fun download(@RequestParam("branch") branch: String): ResponseEntity<Any> {
        val tempPath = Files.createTempDirectory(tempPrefix)
        val zipFile = tempPath.resolve("$OUT_PUT_FOLDER.zip")
        val output = tempPath.resolve(OUT_PUT_FOLDER)
        val javaPath = output.resolve(JAVA_MAIM_FOLDER)
        Files.createDirectories(output)
        val worker = GitProjectWorker(output.toFile(), remoteUrl, branch)
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
            val inputDir = output.toFile()
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                inputDir.walkTopDown().filter { it.absolutePath != inputDir.absolutePath }.forEach { file ->
                    val zipFileName = file.absolutePath.removePrefix(inputDir.absolutePath).removePrefix(File.separator)
                    val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                    zos.putNextEntry(entry)
                    if (file.isFile) {
                        file.inputStream().copyTo(zos)
                    }
                }
            }
        }
        registry.cleanup()
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=generated.zip")
            .body(FileSystemResource(zipFile.toFile()))
    }
}