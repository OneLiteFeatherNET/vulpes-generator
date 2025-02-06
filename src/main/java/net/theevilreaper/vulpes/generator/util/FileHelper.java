package net.theevilreaper.vulpes.generator.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApiStatus.Internal
public final class FileHelper {

    public static void zipFile(@NotNull Path temp, @NotNull Path zipFile) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            File tempFile = temp.toFile();
            List<File> filteredFiles = Files.walk(temp)
                    .map(Path::toFile)
                    .filter(file -> !file.getAbsolutePath().equals(tempFile.getAbsolutePath()))
                    .filter(file -> !file.getName().endsWith(".zip"))
                    .toList();

            for (File file : filteredFiles) {
                String zipFileName = file.getAbsolutePath()
                        .replace(tempFile.getAbsolutePath(), "")
                        .replace(File.separator, "");
                ZipEntry entry = new ZipEntry(zipFileName + (file.isDirectory() ? "/" : ""));
                zos.putNextEntry(entry);
                if (file.isFile()) {
                    Files.copy(file.toPath(), zos);
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FileHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
