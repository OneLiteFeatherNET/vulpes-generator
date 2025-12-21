package net.onelitefeather.vulpes.generator.controller.download;

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
import net.onelitefeather.vulpes.generator.registry.GeneratorRegistry;
import net.onelitefeather.vulpes.generator.util.FileHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.onelitefeather.vulpes.generator.util.Constants.GRADLE_PROPERTIES;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;
import static net.onelitefeather.vulpes.generator.util.Constants.OUT_PUT_FOLDER;
import static net.onelitefeather.vulpes.generator.util.Constants.TEMP_PREFIX;
import static net.onelitefeather.vulpes.generator.util.Constants.ZIP_FILE_NAME;

@Controller("/download")
public class VulpesDownloadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VulpesDownloadController.class);
    private final GeneratorRegistry registry;
    private final GitProjectWorker gitProjectWorker;

    @Inject
    public VulpesDownloadController(
            @NotNull GeneratorRegistry registry,
            @NotNull GitProjectWorker gitProjectWorker
    ) {
        this.registry = registry;
        this.gitProjectWorker = gitProjectWorker;
    }

    @Operation(
            summary = "Download a generated Vulpes code base",
            operationId = "download",
            description = "Downloads a zip file containing the generated Vulpes code base from the specified branch.",
            tags = {"download"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The download was successful",
            content = @Content(
                    mediaType = "application/octet-stream",
                    schema = @Schema(implementation = File.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "An error occurred during download",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class)
            )
    )
    @Get(produces = "application/octet-stream")
    public @NotNull HttpResponse<File> download(
            @QueryValue(value = "branch", defaultValue = "develop") String branch
    ) {
        Path tempPath;
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

        Git git = gitProjectWorker.cloneBaseRepo(output, List.of("refs/heads/" + branch));

        if (git == null) {
            LOGGER.warn("Git clone operation failed for branch: {}", branch);
            return HttpResponse.serverError();
        }

        var zipStream = getClass().getClassLoader().getResourceAsStream(ZIP_FILE_NAME);
        if (zipStream != null) {
            registry.triggerAll(javaPath);
            try {
                Files.createFile(zipFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            FileHelper.zipFile(output, zipFile);
        }
        return HttpResponse.ok(zipFile.toFile());
    }
}
