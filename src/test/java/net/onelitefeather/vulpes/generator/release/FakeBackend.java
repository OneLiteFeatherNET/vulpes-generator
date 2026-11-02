package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;

/**
 * State of the fake backend served by the embedded test server under {@code /fake-backend}. It answers the REST
 * endpoints the generator reads, in the response shapes of backend 3.3.0: objects as they are, lists as pages of
 * {@code {"content":[...],"totalSize":N}}, or as bare arrays when registered with {@link #putBareList}.
 * <p>
 * A path that was never registered answers {@code 404} when it is a project object and an empty page otherwise.
 * Each test resets it in its {@code @BeforeEach}; {@link #reset()} also registers the default project
 * {@link ReleaseTestData#PROJECT_ID} with the key {@code eldoria} and no entities.
 *
 * @author TheMeinerLP
 */
@Singleton
public class FakeBackend {

    private static final Pattern PROJECT_PATH = Pattern.compile("project/[0-9a-f-]{36}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, Supplier<JsonNode>> objects = new ConcurrentHashMap<>();
    private final Map<String, List<JsonNode>> lists = new ConcurrentHashMap<>();
    private final Set<String> bareLists = ConcurrentHashMap.newKeySet();
    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private volatile boolean down;

    /** Clears every registration and every record, then registers the default project without entities. */
    public void reset() {
        objects.clear();
        lists.clear();
        bareLists.clear();
        requestedPaths.clear();
        authorizations.clear();
        down = false;
        putObject("project/" + ReleaseTestData.PROJECT_ID, () -> project(ReleaseTestData.PROJECT_ID, "eldoria", "Eldoria"));
    }

    /** Registers a fixed object answered at the path, for example {@code project/<id>}. */
    public void putObject(String path, JsonNode node) {
        objects.put(path, () -> node);
    }

    /** Registers an object whose content is computed on every read, so tests can change it between reads. */
    public void putObject(String path, Supplier<JsonNode> node) {
        objects.put(path, node);
    }

    /** Registers the entries of a list, answered as pages. */
    public void putList(String path, List<JsonNode> entries) {
        lists.put(path, List.copyOf(entries));
        bareLists.remove(path);
    }

    /** Registers the entries of a list, answered as one bare JSON array, as some backend endpoints do. */
    public void putBareList(String path, List<JsonNode> entries) {
        lists.put(path, List.copyOf(entries));
        bareLists.add(path);
    }

    /**
     * Registers every endpoint of a project from its mapper input, under the same paths the generator reads.
     *
     * @param data the project data
     */
    public void registerProject(ProjectData data) {
        String project = "project/" + data.project().get("id").asText();
        putObject(project, data.project());
        putList(project + "/item", data.items());
        putList(project + "/attribute", data.attributes());
        putList(project + "/font", data.fonts());
        putList(project + "/sound", data.sounds());
        putList(project + "/expression", data.expressions());
        putList(project + "/expression-function", data.expressionFunctions());
        putList(project + "/asset", data.assets());
        putList(project + "/advancement", data.advancements());
        for (JsonNode item : data.items()) {
            String id = item.get("id").asText();
            putList("item/" + id + "/components", data.components().getOrDefault(id, List.of()));
            putList("item/" + id + "/lore", data.lore().getOrDefault(id, List.of()));
        }
        for (JsonNode font : data.fonts()) {
            String id = font.get("id").asText();
            putList("font/chars/" + id, data.glyphRows().getOrDefault(id, List.of()));
        }
        for (JsonNode sound : data.sounds()) {
            String id = sound.get("id").asText();
            putList("sound/" + id + "/sources", data.sources().getOrDefault(id, List.of()));
        }
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
     * @param size          the requested page size, or null
     * @param page          the requested page, or null
     * @return the status and the JSON body
     */
    FakeReply serve(String path, String authorization, Integer size, Integer page) {
        requestedPaths.add(path);
        authorizations.add(authorization);
        if (down) {
            return new FakeReply(503, "");
        }
        Supplier<JsonNode> object = objects.get(path);
        if (object != null) {
            return new FakeReply(200, object.get().toString());
        }
        if (PROJECT_PATH.matcher(path).matches()) {
            return new FakeReply(404, "");
        }
        List<JsonNode> entries = lists.getOrDefault(path, List.of());
        if (bareLists.contains(path)) {
            return new FakeReply(200, toArray(entries).toString());
        }
        int pageSize = size == null ? Integer.MAX_VALUE : size;
        int from = Math.min(page == null ? 0 : page * pageSize, entries.size());
        int to = (int) Math.min((long) from + pageSize, entries.size());
        ObjectNode body = JSON.createObjectNode();
        body.set("content", toArray(entries.subList(from, to)));
        body.put("totalSize", entries.size());
        return new FakeReply(200, body.toString());
    }

    private static ArrayNode toArray(List<JsonNode> entries) {
        ArrayNode array = JSON.createArrayNode();
        entries.forEach(array::add);
        return array;
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
