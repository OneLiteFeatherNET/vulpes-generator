package net.onelitefeather.vulpes.generator.release;

/**
 * A local build of a project, ready to be downloaded.
 *
 * @param projectKey     the project key, which names the file of the download
 * @param zip            the zip bytes: the two metadata entries of the project and, when the project has pack settings,
 *                       its resource pack
 * @param contentProblems the number of content problems of the snapshot, zero when the content is valid
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public record LocalBuild(String projectKey, byte[] zip, int contentProblems) {

    /**
     * Creates a local build whose content has no problem.
     *
     * @param projectKey the project key
     * @param zip        the zip bytes
     */
    public LocalBuild(String projectKey, byte[] zip) {
        this(projectKey, zip, 0);
    }
}
