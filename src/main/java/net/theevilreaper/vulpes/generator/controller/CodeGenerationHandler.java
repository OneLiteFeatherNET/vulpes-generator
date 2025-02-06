package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.generation.GitProjectWorker;
import net.theevilreaper.vulpes.generator.properties.GitlabProperties;
import net.theevilreaper.vulpes.generator.registry.RegistryProvider;
import net.theevilreaper.vulpes.generator.util.BranchFilter;
import net.theevilreaper.vulpes.generator.util.FileHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
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

import static java.util.stream.Collectors.toList;
import static net.theevilreaper.vulpes.generator.util.Constants.*;

@Controller("/generator")
public class CodeGenerationHandler {

    private final RegistryProvider registryProvider;
    private final GitlabProperties gitlabProperties;

    @Inject
    public CodeGenerationHandler(RegistryProvider registryProvider, GitlabProperties gitlabProperties) {
        this.registryProvider = registryProvider;
        this.gitlabProperties = gitlabProperties;
    }

    @Get(value = "/branches", produces = "application/json")
    public HttpResponse<List<String>> getBranches(
            @QueryValue(value = "full", defaultValue = "false") boolean full
            ) {
        List<String> refs = null;
        try {
            refs = Git.lsRemoteRepository()
                    .setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(
                                    gitlabProperties.user(),
                                    gitlabProperties.password()
                            )
                    )
                    .setHeads(true)
                    .setRemote(gitlabProperties.remoteUrl())
                    .call()
                    .stream().map(Ref::getName).collect(toList());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        var filtered = BranchFilter.filterBranches(refs, string -> string.contains("/renovate"));

        if (full) {
            return HttpResponse.ok(filtered).contentType(MediaType.APPLICATION_JSON);
        } else {
            var branches =
                    BranchFilter.filterBranches(refs, string -> !string.contains("/renovate"))
                            .stream().map(string -> string.replace("refs/heads/", "")).collect(toList());
            return HttpResponse.ok(branches).contentType(MediaType.APPLICATION_JSON);
        }
    }

    @Get(value = "/generate", produces = "application/json")
    public HttpResponse<Object> generate(
            @QueryValue(value = "branch") String branch,
            @QueryValue(value = "image") String image
    ) throws IOException {
        var tempPath = Files.createTempDirectory(TEMP_PREFIX);
        var output = tempPath.resolve(OUT_PUT_FOLDER);
        var tempGitlabCi = tempPath.resolve(GITLAB_CI_YML);
        var basePath = Path.of("generated");
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
            variables.put("GENERATION_URL", gitlabProperties.deployUrl());
            objects.put("variables", variables);
            Files.write(gitlabCiFile, yaml.dumpAsMap(objects).getBytes());
        }
        var rawGit =
                Git.cloneRepository().setURI(gitlabProperties.pipelineUrl()).setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(
                                gitlabProperties.user(),
                                gitlabProperties.password()
                        )
                ).setDirectory(output.toFile()).setCloneAllBranches(true);
        Git git = null;
        try {
            git = rawGit.call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
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
        commit.setAuthor(COMMIT_NAME, COMMIT_MAIL);
        commit.setMessage(COMMIT_MESSAGE);
        commit.setSign(false);
        try {
            commit.call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        var push = git.push();
        push.setCredentialsProvider(
                new UsernamePasswordCredentialsProvider(
                        gitlabProperties.user(),
                        gitlabProperties.password()
                )
        );
        try {
            push.call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

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

        var worker = new GitProjectWorker(output.toFile(), gitlabProperties.remoteUrl(), branch, gitlabProperties.user(), gitlabProperties.password());
        worker.cloneAndCheckout();
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
