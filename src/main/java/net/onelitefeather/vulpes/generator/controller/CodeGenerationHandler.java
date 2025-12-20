package net.onelitefeather.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.git.GitWorker;
import net.onelitefeather.vulpes.generator.domain.configuration.CommitConfiguration;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;

import static net.onelitefeather.vulpes.generator.util.Constants.*;

@Controller("/generator")
public class CodeGenerationHandler {

    private final CommitConfiguration commitConfiguration;
    private final GitWorker gitWorker;

    @Inject
    public CodeGenerationHandler(
            @NotNull CommitConfiguration commitConfiguration,
            @NotNull GitWorker gitWorker
    ) {
        this.commitConfiguration = commitConfiguration;
        this.gitWorker = gitWorker;
    }

    @Operation(
            summary = "Generate a new vulpes version",
            operationId = "generate",
            description = "Generates a new code base for vulpes based on the provided branch",
            tags = {"generation"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The generation was successful",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Object.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "An error occurred during generation",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class)
            )
    )
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

        /*var yaml = new Yaml();
        try (BufferedReader in = Files.newBufferedReader(tempGitlabCi)) {
            Map<String, Object> objects = yaml.load(in);
            if (image != null) {
                objects.put("image", image);
            }
            Map<String, Object> variables = (Map<String, Object>) objects.get("variables");
            variables.put("BRANCH", branch);
            variables.put("GENERATION_URL", "fix_url_later");
            objects.put("variables", variables);
            Files.write(gitlabCiFile, yaml.dumpAsMap(objects).getBytes());
        }*/

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
        commit.setAuthor(this.commitConfiguration.author(), this.commitConfiguration.mail());
        commit.setMessage(this.commitConfiguration.message());
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
}
