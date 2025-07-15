package net.onelitefeather.vulpes.generator.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.onelitefeather.vulpes.generator.util.Constants.EMPTY_STRING;

/**
 * Utility class to zip a given {@link Path} into another one.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public final class FileHelper {

    private static final Logger FILE_LOGGER = LoggerFactory.getLogger(FileHelper.class);

    /**
     * Compresses the contents of a given directory into a ZIP file.
     * <p>
     * This method recursively collects all files from the specified {@code temp} directory
     * (excluding itself and existing ZIP files) and writes them into the specified {@code zipFile}.
     * </p>
     *
     * @param temp    The directory containing files to be compressed.
     * @param zipFile The output ZIP file where compressed data will be stored.
     * @throws NullPointerException if either {@code temp} or {@code zipFile} is null.
     */
    public static void zipFile(@NotNull Path temp, @NotNull Path zipFile) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile));
             Stream<Path> paths = Files.walk(temp)) {

            Path tempAbsolutePath = temp.toAbsolutePath();

            List<Path> filteredFiles = paths
                    .filter(path -> !path.equals(temp))
                    .filter(path -> !path.getFileName().toString().endsWith(".zip"))
                    .toList();

            for (Path file : filteredFiles) {
                Path filePath = file.toAbsolutePath();
                String zipFileName = tempAbsolutePath.relativize(filePath).toString().replace("\\", File.separator);
                boolean isDirectory = Files.isDirectory(file);
                ZipEntry entry = new ZipEntry(zipFileName + (isDirectory ? File.separator : EMPTY_STRING));
                zos.putNextEntry(entry);

                if (Files.isRegularFile(file)) {
                    Files.copy(file, zos);
                }
                zos.closeEntry();
            }
        } catch (IOException exception) {
            FILE_LOGGER.error("Error while zipping files", exception);
        }
    }

    private FileHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
