package net.onelitefeather.vulpes.generator.service.impl;

import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.service.FileService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of the FileService interface.
 */
@Singleton
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
    private static final String GENERATION_DIR = "generation";
    private static final String MODELS_PACKAGE_PATH = "net/onelitefeather/vulpes/models";

    @Override
    public Path createGenerationDirectory(String branchName) {
        try {
            String dirName = GENERATION_DIR + "/" + branchName + "-" + UUID.randomUUID();
            Path generationPath = Path.of(dirName);
            Files.createDirectories(generationPath);
            logger.info("Created generation directory: {}", generationPath);
            return generationPath;
        } catch (IOException e) {
            logger.error("Failed to create generation directory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create generation directory", e);
        }
    }

    @Override
    public void deleteGenerationDirectory(Path generationDirectory) {
        try {
            if (Files.exists(generationDirectory)) {
                Files.walkFileTree(generationDirectory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                logger.info("Deleted generation directory: {}", generationDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to delete generation directory: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates a ZIP archive of the generated code using NIO.
     *
     * @param sourcePath the path to the source directory
     * @return the path to the created ZIP file
     */
    public Path createZipArchive(Path sourcePath) {
        try {
            Path zipPath = Path.of(sourcePath.toString() + ".zip");

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                    Files.newOutputStream(zipPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {

                Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        // Create a zip entry
                        String entryName = sourcePath.relativize(file).toString().replace('\\', '/');
                        zipOutputStream.putNextEntry(new ZipEntry(entryName));

                        // Copy file content to zip output stream
                        Files.copy(file, zipOutputStream);

                        // Close the entry
                        zipOutputStream.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        logger.warn("Failed to visit file: {}", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            logger.info("Created ZIP archive: {}", zipPath);
            return zipPath;
        } catch (IOException e) {
            logger.error("Failed to create ZIP archive: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create ZIP archive", e);
        }
    }

    /**
     * Deletes the models package directory from the cloned repository.
     *
     * @param repoPath the path to the cloned repository
     * @return true if the directory was deleted, false otherwise
     */
    public boolean deleteModelsPackage(Path repoPath) {
        try {
            Path modelsPath = repoPath.resolve("src/main/java").resolve(MODELS_PACKAGE_PATH);
            if (Files.exists(modelsPath)) {
                Files.walkFileTree(modelsPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                logger.info("Deleted models package directory: {}", modelsPath);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to delete models package directory: {}", e.getMessage(), e);
            return false;
        }
    }
}