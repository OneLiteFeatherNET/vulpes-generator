package net.onelitefeather.vulpes.generator.release;

/**
 * A local build of a project, ready to be downloaded.
 *
 * @param projectKey the project key, which names the file of the download
 * @param zip        the zip bytes, with exactly the two metadata entries of the project
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public record LocalBuild(String projectKey, byte[] zip) {
}
