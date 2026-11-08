package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;

/**
 * State of the fake backend served by the embedded test server under {@code /fake-backend}. It answers the two
 * endpoints a release reads: the project object {@code project/<id>} and the content export
 * {@code project/<id>/content}. Every other path answers {@code 404}.
 * <p>
 * Each test resets it in its {@code @BeforeEach}; {@link #reset()} also registers the default project
 * {@link ReleaseTestData#PROJECT_ID} with the key {@code eldoria}, an empty export and no entities.
 *
 * @author TheMeinerLP
 */
@Singleton
public class FakeBackend {

    private final Map<String, FakeReply> routes = new ConcurrentHashMap<>();
    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private volatile boolean down;

    /** Clears every registration and every record, then registers the default project without entities. */
    public void reset() {
        routes.clear();
        requestedPaths.clear();
        authorizations.clear();
        down = false;
        registerProject(ProjectFixture.project(ReleaseTestData.PROJECT_ID, "eldoria", "Eldoria").build());
    }

    /** Registers a fixed object answered with {@code 200} at the path, for example {@code project/<id>}. */
    public void putObject(String path, JsonNode node) {
        routes.put(path, new FakeReply(200, node.toString()));
    }

    /** Registers the content export answered at {@code project/<id>/content}. */
    public void putExport(java.util.UUID projectId, JsonNode export) {
        putObject(exportPath(projectId), export);
    }

    /** Registers an arbitrary reply at the path, for failures and malformed bodies. */
    public void putReply(String path, FakeReply reply) {
        routes.put(path, reply);
    }

    /**
     * Registers the project object and the content export of one project, built from its mapper input.
     *
     * @param data the project data
     */
    public void registerProject(ProjectData data) {
        String id = data.project().get("id").asText();
        putObject("project/" + id, data.project());
        putObject("project/" + id + "/content", ContentExport.of(data));
    }

    /** Makes the backend answer 503 for every request, as if it were down. */
    public void setDown(boolean down) {
        this.down = down;
    }

    /** Returns every requested path in order, without the query string. */
    public List<String> requestedPaths() {
        return List.copyOf(requestedPaths);
    }

    /** Returns how many requests reached the backend for exactly this path. */
    public int requestCount(String path) {
        return (int) requestedPaths.stream().filter(path::equals).count();
    }

    /** Returns how many requests reached the backend since the last reset. */
    public int requestCount() {
        return requestedPaths.size();
    }

    /** Returns the Authorization header of every request, null where none was sent. */
    public List<String> authorizations() {
        return java.util.Collections.unmodifiableList(authorizations);
    }

    /** Returns the Authorization header of the last request, or null. */
    public String lastAuthorization() {
        return authorizations.isEmpty() ? null : authorizations.get(authorizations.size() - 1);
    }

    /**
     * Answers one request and records it.
     *
     * @param path          the path below {@code /fake-backend}
     * @param authorization the Authorization header, or null
     * @return the status and the body
     */
    FakeReply serve(String path, String authorization) {
        requestedPaths.add(path);
        authorizations.add(authorization);
        if (down) {
            return new FakeReply(503, "");
        }
        FakeReply reply = routes.get(path);
        if (reply != null) {
            return reply;
        }
        return new FakeReply(404, "");
    }

    /** The path of the content export of a project. */
    static String exportPath(java.util.UUID projectId) {
        return "project/" + projectId + "/content";
    }

    /**
     * Builds a project object of the backend shape.
     *
     * @param id          the project id
     * @param key         the project key
     * @param displayName the display name
     * @return the project object
     */
    static JsonNode project(java.util.UUID id, String key, String displayName) {
        return obj("id", id.toString(), "key", key, "displayName", displayName, "description", "", "labor", false);
    }

    /**
     * A reply of the fake backend.
     *
     * @param status the HTTP status
     * @param body   the JSON body, empty for status-only replies
     */
    record FakeReply(int status, String body) {
    }
}
