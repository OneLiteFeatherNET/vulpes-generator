package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.git.GitWorker;
import net.theevilreaper.vulpes.generator.properties.CommitProperties;
import net.theevilreaper.vulpes.generator.registry.RegistryProvider;
import net.theevilreaper.vulpes.generator.util.BranchFilter;
import net.theevilreaper.vulpes.generator.util.FileHelper;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.nio.file.Files;
import java.util.Map;

import static net.theevilreaper.vulpes.generator.util.Constants.*;

@Controller("/generator")
public class CodeGenerationHandler {

    private final RegistryProvider registryProvider;
    private final CommitProperties commitProperties;
    private final GitWorker gitWorker;

    @Inject
    public CodeGenerationHandler(
            @NotNull RegistryProvider registryProvider,
            @NotNull CommitProperties commitProperties,
            @NotNull GitWorker gitWorker
    ) {
        this.registryProvider = registryProvider;
        this.commitProperties = commitProperties;
        this.gitWorker = gitWorker;
    }

    @Get(value = "/branches", produces = "application/json")
    public HttpResponse<List<String>> getBranches(@QueryValue(value = "full", defaultValue = "false") boolean full) {
        List<String> gitRefs = gitWorker.getGitRefs();
        List<String> filtered = BranchFilter.filterBranches(gitRefs, string -> !string.contains("/renovate"));

        if (full) {
            return HttpResponse.ok(filtered).contentType(MediaType.APPLICATION_JSON);
        }
        List<String> branches = filtered.stream().map(string -> string.replace("refs/heads/", EMPTY_STRING)).toList();
        return HttpResponse.ok(branches).contentType(MediaType.APPLICATION_JSON);
    }

    @Get(value = "/generate", produces = "application/json")
    public HttpResponse<Object> generate(
            @QueryValue(value = "branch") String branch,
            @QueryValue(value = "image") String image
    ) throws IOException {
        var tempPath = Files.createTempDirectory(TEMP_PREFIX);
        var output = tempPath.resolve(OUT_PUT_FOLDER);
        var tempGitlabCi = tempPath.resolve(GITLAB_CI_YML);
        Files.copy(
                getClass().getClassLoader().getResourceAsStream(GITLAB_CI_YML),
                tempGitlabCi
        );
        Files.createDirectories(output);
        var gitlabCiFile = output.resolve(GITLAB_CI_YML);

        var yaml = new Yaml();
        try (BufferedReader in = Files.newBufferedReader(tempGitlabCi)) {
            Map<String, Object> objects = yaml.load(in);
            if (image != null) {
                objects.put("image", image);
            }
            Map<String, Object> variables = (Map<String, Object>) objects.get("variables");
            variables.put("BRANCH", branch);
            variables.put("GENERATION_URL", gitWorker.getDeployUrl());
            objects.put("variables", variables);
            Files.write(gitlabCiFile, yaml.dumpAsMap(objects).getBytes());
        }

        CloneCommand cloneCommand = gitWorker.getCloneCommand(output);
        Git git = null;
        try {
            git = cloneCommand.call();
        } catch (GitAPIException apiException) {
            apiException.printStackTrace();
        }

        Files.copy(tempGitlabCi, gitlabCiFile, StandardCopyOption.REPLACE_EXISTING);
        var add = git.add();
        add.addFilepattern(gitlabCiFile.toString());
        try {
            add.call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        var commit = git.commit();
        commit.setAuthor(this.commitProperties.author(), this.commitProperties.mail());
        commit.setMessage(this.commitProperties.message());
        commit.setSign(false);
        try {
            commit.call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        PushCommand push = git.push();
        gitWorker.push(push);
        return HttpResponse.ok().contentType(MediaType.APPLICATION_JSON);
    }

    @Get(value = "/download", produces = "application/octet-stream")
    public @NotNull HttpResponse<File> download(
            @QueryValue(value = "branch", defaultValue = "master") String branch
    ) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempDirectory(TEMP_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var output = tempPath.resolve(OUT_PUT_FOLDER);
        var zipFile = tempPath.resolve(OUT_PUT_FOLDER + ".zip");
        var javaPath = output.resolve(JAVA_MAIM_FOLDER);

        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        gitWorker.cloneAndCheckout(branch, output);
        var zipStream = getClass().getClassLoader().getResourceAsStream(ZIP_FILE_NAME);
        if (zipStream != null) {
            var buildGradle = output.resolve(GRADLE_PROPERTIES);
            applyVulpesData(buildGradle);
            registryProvider.getGeneratorRegistry().triggerAll(javaPath);
            try {
                Files.createFile(zipFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            FileHelper.zipFile(output, zipFile);
        }
        return HttpResponse.ok(zipFile.toFile()).contentType(MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * Applies some vulpes relevant data to a file from gradle.
     *
     * @param
     */
    private void applyVulpesData(Path buildGradle) {
        /*buildGradle.let {
            val properties = Properties()
            properties.load(it.reader())
            properties["vulpesGroupId"] = BASE_PACKAGE
            properties["vulpesBaseUrl"] = this.properties.dependencyUrl
            properties["vulpesVersion"] = version
            properties.store(it.writer(), "Generated Config")
        }*/
    }
}
