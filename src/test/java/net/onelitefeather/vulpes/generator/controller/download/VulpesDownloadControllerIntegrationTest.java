package net.onelitefeather.vulpes.generator.controller.download;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.domain.client.GithubService;
import net.onelitefeather.vulpes.generator.domain.generation.GenerationResponse;
import net.onelitefeather.vulpes.generator.domain.generation.VulpesGenerationService;
import net.onelitefeather.vulpes.generator.domain.generation.exception.GenerationException;
import net.onelitefeather.vulpes.generator.util.FileHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MicronautTest
@DisplayName("Vulpes Download Controller Integration Tests")
class VulpesDownloadControllerIntegrationTest {

    @Inject
    @Client("/download")
    HttpClient client;

    @Inject
    VulpesGenerationService generationService;

    @Inject
    GithubService githubService;

    @MockBean(GithubService.class)
    GithubService githubService() {
        return mock(GithubService.class);
    }

    @MockBean(VulpesGenerationService.class)
    VulpesGenerationService generationService() {
        return mock(VulpesGenerationService.class);
    }

    @DisplayName("Should return HTTP 400 when no branches are available in repository")
    @Test
    void testShouldReturnBadRequestWhenNoBranchesAvailable() {
        // Given
        when(githubService.getBranches()).thenReturn(Collections.emptyList());

        // When & Then
        HttpRequest<Object> request = HttpRequest.GET("/?branch=develop");

        BlockingHttpClient blockingHttpClient = client.toBlocking();
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> blockingHttpClient.exchange(request, GenerationResponse.GenerationErrorResponseDTO.class)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Bad Request"));
    }

    @DisplayName("Should return HTTP 400 when requested branch does not exist in repository")
    @Test
    void testShouldReturnBadRequestWhenBranchNotFound() {
        // Given
        when(githubService.getBranches()).thenReturn(Arrays.asList("main", "develop"));

        // When & Then
        HttpRequest<Object> request = HttpRequest.GET("/?branch=invalid");

        BlockingHttpClient blockingHttpClient = client.toBlocking();
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> blockingHttpClient.exchange(request, GenerationResponse.GenerationErrorResponseDTO.class)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Bad Request"));
    }

    @DisplayName("Should return HTTP 500 when generation process fails")
    @Test
    void testShouldReturnServerErrorWhenGenerationFails() throws GenerationException {
        // Given
        when(githubService.getBranches()).thenReturn(List.of("develop"));
        when(generationService.getVulpesGeneration("develop"))
                .thenThrow(new GenerationException(
                        GenerationException.Type.VULPES,
                        "Generation failed"
                ));

        // When & Then
        HttpRequest<Object> request = HttpRequest.GET("/?branch=develop");

        BlockingHttpClient blockingHttpClient = client.toBlocking();
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> blockingHttpClient.exchange(request, GenerationResponse.GenerationErrorResponseDTO.class)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @DisplayName("Should return HTTP 200 with zip file when generation succeeds")
    @Test
    void testShouldReturnOkWithZipFileWhenGenerationSucceeds() throws Exception {
        // Given
        Path tempDir = Files.createTempDirectory("test");
        Path outputPath = tempDir.resolve("output");
        Files.createDirectories(outputPath);

        when(githubService.getBranches()).thenReturn(List.of("develop"));
        when(generationService.getVulpesGeneration("develop")).thenReturn(outputPath);

        try (MockedStatic<FileHelper> fileHelperMock = mockStatic(FileHelper.class)) {
            fileHelperMock.when(() -> FileHelper.zipFile(any(Path.class), any(Path.class)))
                    .thenAnswer(invocation -> {
                        Path zipPath = invocation.getArgument(1);
                        Files.createFile(zipPath);
                        return null;
                    });

            HttpRequest<Object> request = HttpRequest.GET("/?branch=develop");
            HttpResponse<byte[]> response =
                    client.toBlocking().exchange(request, byte[].class);

            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.getHeaders().contains("Content-Disposition"));
        }
    }
}