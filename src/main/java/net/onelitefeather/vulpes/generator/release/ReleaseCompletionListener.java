package net.onelitefeather.vulpes.generator.release;

/**
 * Port that is notified after a release was published successfully.
 * It is never called for a rejected or failed release.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ReleaseCompletionListener {

    /**
     * Called once after a successful publish.
     *
     * @param coordinates the coordinates of the published JAR
     * @param revision    the revision the JAR was built from
     */
    void completed(ContentCoordinates coordinates, String revision);
}
