package net.onelitefeather.vulpes.generator.controller.download;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.types.files.SystemFile;
import net.onelitefeather.vulpes.generator.domain.client.GithubService;
import net.onelitefeather.vulpes.generator.domain.generation.GenerationResponse;
import net.onelitefeather.vulpes.generator.domain.generation.VulpesGenerationService;
import net.onelitefeather.vulpes.generator.domain.generation.exception.GenerationException;
import net.onelitefeather.vulpes.generator.util.FileHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vulpes DownloadController Tests")
class VulpesDownloadControllerTest {

    @Mock
    private VulpesGenerationService generationService;

    @Mock
    private GithubService githubService;

    @InjectMocks
    private VulpesDownloadController controller;

    @TempDir
    Path tempDir;

    private List<String> activeBranches;

    @BeforeEach
    void setUp() {
        activeBranches = Arrays.asList("develop", "main", "feature/test");
    }

    @DisplayName("Should return bad request when no branches are available")
    @Test
    void testDownloadShouldReturnBadRequestWhenNoBranches() {
        // Given
        when(githubService.getBranches()).thenReturn(Collections.emptyList());

        HttpResponse<Object> response = controller.download("develop");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertInstanceOf(GenerationResponse.GenerationErrorResponseDTO.class, response.body());
        GenerationResponse.GenerationErrorResponseDTO error =
                (GenerationResponse.GenerationErrorResponseDTO) response.body();
        assertEquals("No branches available", error.errorMessage());

        verify(githubService).getBranches();
        verifyNoInteractions(generationService);
    }

    @DisplayName("Should return bad request when requested branch does not exist")
    @Test
    void testExceptionThrowWhenNoBranchesExists() {
        when(githubService.getBranches()).thenReturn(activeBranches);

        HttpResponse<Object> response = controller.download("nonexistent");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertInstanceOf(GenerationResponse.GenerationErrorResponseDTO.class, response.body());
        GenerationResponse.GenerationErrorResponseDTO error =
                (GenerationResponse.GenerationErrorResponseDTO) response.body();
        assertTrue(error.errorMessage().contains("nonexistent"));
        assertTrue(error.errorMessage().contains("does not exist"));

        verify(githubService).getBranches();
        verifyNoInteractions(generationService);
    }

    @DisplayName("Should successfully return zip file when generation succeeds")
    @Test
    void testDownloadShouldReturnZipFileOnSuccess() throws Exception {
        String branch = "develop";
        Path outputPath = tempDir.resolve("output");
        Files.createDirectories(outputPath);

        when(githubService.getBranches()).thenReturn(activeBranches);
        when(generationService.getVulpesGeneration(branch)).thenReturn(outputPath);

        try (MockedStatic<FileHelper> fileHelperMock = mockStatic(FileHelper.class)) {
            fileHelperMock.when(() -> FileHelper.zipFile(any(Path.class), any(Path.class)))
                    .thenAnswer(invocation -> {
                        Path zipPath = invocation.getArgument(1);
                        Files.createFile(zipPath);
                        return null;
                    });

            HttpResponse<Object> response = controller.download(branch);

            assertEquals(HttpStatus.OK, response.getStatus());
            assertInstanceOf(SystemFile.class, response.body());

            verify(githubService).getBranches();
            verify(generationService).getVulpesGeneration(branch);
            fileHelperMock.verify(() -> FileHelper.zipFile(eq(outputPath), any(Path.class)));
        }
    }

    @DisplayName("Should return server error when generation fails")
    @Test
    void testDownloadShouldReturnAServerErrorOnAnyException() throws GenerationException {
        String branch = "develop";
        String errorMessage = "Git clone failed";

        when(githubService.getBranches()).thenReturn(activeBranches);
        when(generationService.getVulpesGeneration(branch))
                .thenThrow(new GenerationException(
                        GenerationException.Type.VULPES,
                        errorMessage
                ));

        HttpResponse<Object> response = controller.download(branch);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        assertInstanceOf(GenerationResponse.GenerationErrorResponseDTO.class, response.body());
        GenerationResponse.GenerationErrorResponseDTO error =
                (GenerationResponse.GenerationErrorResponseDTO) response.body();
        assertTrue(error.errorMessage().contains(errorMessage));

        verify(githubService).getBranches();
        verify(generationService).getVulpesGeneration(branch);
    }

    @DisplayName("Should return server error when zip file creation fails")
    @Test
    void testExceptionThrowWhenFileCantBeCreated() throws Exception {
        String branch = "develop";
        Path outputPath = tempDir.resolve("output");
        Files.createDirectories(outputPath);

        when(githubService.getBranches()).thenReturn(activeBranches);
        when(generationService.getVulpesGeneration(branch)).thenReturn(outputPath);

        try (MockedStatic<FileHelper> fileHelperMock = mockStatic(FileHelper.class)) {
            fileHelperMock.when(() -> FileHelper.zipFile(any(Path.class), any(Path.class)))
                    .thenThrow(new RuntimeException("Zip creation failed"));

            HttpResponse<Object> response = controller.download(branch);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            assertInstanceOf(GenerationResponse.GenerationErrorResponseDTO.class, response.body());
            GenerationResponse.GenerationErrorResponseDTO error =
                    (GenerationResponse.GenerationErrorResponseDTO) response.body();
            assertTrue(error.errorMessage().contains("Failed to create zip file"));

            verify(githubService).getBranches();
            verify(generationService).getVulpesGeneration(branch);
        }
    }

    @DisplayName("Should use default branch 'develop' when no branch parameter is provided")
    @Test
    void testDownloadShouldUseDefaultBranchWhenNoBranchIsGiven() throws Exception {
        Path outputPath = tempDir.resolve("output");
        Files.createDirectories(outputPath);

        when(githubService.getBranches()).thenReturn(activeBranches);
        when(generationService.getVulpesGeneration("develop")).thenReturn(outputPath);

        try (MockedStatic<FileHelper> fileHelperMock = mockStatic(FileHelper.class)) {
            fileHelperMock.when(() -> FileHelper.zipFile(any(Path.class), any(Path.class)))
                    .thenAnswer(invocation -> {
                        Path zipPath = invocation.getArgument(1);
                        Files.createFile(zipPath);
                        return null;
                    });

            HttpResponse<Object> response = controller.download("develop");
            assertEquals(HttpStatus.OK, response.getStatus());
            verify(generationService).getVulpesGeneration("develop");
        }
    }
}
