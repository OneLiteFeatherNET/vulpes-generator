package net.onelitefeather.vulpes.generator.release;

/**
 * The Maven coordinates of a content data JAR.
 *
 * @param groupId    the Maven group id, always {@link #GROUP_ID} for content releases
 * @param artifactId the Maven artifact id, the project name
 * @param version    the release version
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public record ContentCoordinates(String groupId, String artifactId, String version) {

    /** The Maven group id of all content data JARs. */
    public static final String GROUP_ID = "net.onelitefeather.vulpes.content";

    /**
     * Creates the coordinates of a content release.
     *
     * @param project the project name, used as artifact id
     * @param version the release version
     * @return the coordinates in {@link #GROUP_ID}
     */
    public static ContentCoordinates forRelease(String project, String version) {
        return new ContentCoordinates(GROUP_ID, project, version);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
