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
import net.theevilreaper.vulpes.generator.util.FileHelper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.theevilreaper.vulpes.generator.util.Constants.GRADLE_PROPERTIES;
import static net.theevilreaper.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;
import static net.theevilreaper.vulpes.generator.util.Constants.OUT_PUT_FOLDER;
import static net.theevilreaper.vulpes.generator.util.Constants.TEMP_PREFIX;
import static net.theevilreaper.vulpes.generator.util.Constants.ZIP_FILE_NAME;

@Controller("/download")
public class DownloadController {

    private final RegistryProvider registryProvider;
    private final GitWorker gitWorker;

    @Inject
    public DownloadController(
            @NotNull RegistryProvider registryProvider,
            @NotNull CommitProperties commitProperties,
            @NotNull GitWorker gitWorker
    ) {
        this.registryProvider = registryProvider;
        this.gitWorker = gitWorker;
    }

    @Operation(
            summary = "Download a generated vulpes code base",
            description = "Downloads a zip file containing the generated vulpes code base from the specified branch.",
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
