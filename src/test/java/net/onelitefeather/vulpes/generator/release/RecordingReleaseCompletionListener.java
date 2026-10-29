package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Replaces the logging listener in the test context and records every completion.
 *
 * @author TheMeinerLP
 */
@Singleton
@Replaces(LoggingReleaseCompletionListener.class)
public class RecordingReleaseCompletionListener implements ReleaseCompletionListener {

    private final List<String> completed = new CopyOnWriteArrayList<>();

    @Override
    public void completed(ContentCoordinates coordinates, String revision) {
        completed.add(coordinates + " @ " + revision);
    }

    public void reset() {
        completed.clear();
    }

    public List<String> completed() {
        return List.copyOf(completed);
    }
}
