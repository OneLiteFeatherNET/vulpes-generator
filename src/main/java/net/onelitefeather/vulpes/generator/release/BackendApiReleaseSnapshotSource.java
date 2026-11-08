package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.UNKNOWN_PROJECT;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT;

/**
 * Assembles the format 1 snapshot of a project from the content export of the backend,
 * {@code GET /project/{projectId}/content}. The backend answers the whole project in one consistent read, so a
 * release reads it once and never mixes two states of the project.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BackendApiReleaseSnapshotSource implements ReleaseSnapshotSource, ProjectSource {

    private static final Pattern PROJECT_KEY = Pattern.compile("[a-z][a-z0-9-]*");

    private final BackendApiClient backend;
    private final ProjectSnapshotMapper mapper;

    /**
     * Creates the source.
     *
     * @param client            the HTTP client of the {@code vulpes-backend} service
     * @param backendUrl        the base URL of the backend, for example {@code http://vulpes-backend:8080}
     * @param token             the bearer token for the backend, sent with every request when it is not blank
     * @param foreignNamespaces the first key segments that are namespaces of their own
     */
    @Inject
    public BackendApiReleaseSnapshotSource(@Client(id = "vulpes-backend") HttpClient client,
                                           @Value("${vulpes.backend.url}") String backendUrl,
                                           @Value("${vulpes.backend.token:}") String token,
                                           @Property(name = "vulpes.release.foreign-namespaces") List<String> foreignNamespaces) {
        this.backend = new BackendApiClient(client, backendUrl, token);
        this.mapper = new ProjectSnapshotMapper(foreignNamespaces);
    }

    @Override
    public byte[] fetch(UUID projectId) {
        return assemble(projectId).bytes();
    }

    @Override
    public String projectKey(UUID projectId) {
        JsonNode project = backend.find("/project/" + projectId).orElseThrow(() -> new ReleaseRejectedException(
                UNKNOWN_PROJECT, "backend does not know project " + projectId));
        String key = project.path("key").asText("");
        if (!PROJECT_KEY.matcher(key).matches()) {
            throw new ReleaseRejectedException(UNSUPPORTED_CONTENT, "the backend key '" + key
                    + "' of project " + projectId + " is not a valid project key");
        }
        return key;
    }

    /**
     * Assembles the snapshot of a project from one read of its content export, with the key of the export.
     *
     * @param projectId the id of the project
     * @return the snapshot
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#UNKNOWN_PROJECT},
     *                                  {@link ReleaseRejectedException.Reason#SOURCE_UNAVAILABLE} or
     *                                  {@link ReleaseRejectedException.Reason#UNSUPPORTED_CONTENT}
     */
    public AssembledSnapshot assemble(UUID projectId) {
        JsonNode export = backend.find("/project/" + projectId + "/content").orElseThrow(() ->
                new ReleaseRejectedException(UNKNOWN_PROJECT, "backend does not know project " + projectId));
        return mapper.map(toProjectData(export));
    }

    /**
     * Groups the children of the export by the id of their parent. The export holds them inside their parents, so
     * the order of every list is the order of the export.
     */
    private static ProjectData toProjectData(JsonNode export) {
        if (!export.isObject()) {
            throw unavailable("the content export is not a JSON object");
        }
        JsonNode project = export.get("project");
        if (project == null || !project.isObject()) {
            throw unavailable("the content export has no project object");
        }
        List<JsonNode> items = array(export, "items");
        Map<String, List<JsonNode>> components = new LinkedHashMap<>();
        Map<String, List<JsonNode>> lore = new LinkedHashMap<>();
        for (JsonNode item : items) {
            String id = item.path("id").asText();
            components.put(id, array(item, "components"));
            lore.put(id, array(item, "lore"));
        }
        List<JsonNode> fonts = array(export, "fonts");
        Map<String, List<JsonNode>> glyphRows = new LinkedHashMap<>();
        for (JsonNode font : fonts) {
            glyphRows.put(font.path("id").asText(), array(font, "chars"));
        }
        List<JsonNode> sounds = array(export, "sounds");
        Map<String, List<JsonNode>> sources = new LinkedHashMap<>();
        for (JsonNode sound : sounds) {
            sources.put(sound.path("id").asText(), array(sound, "sources"));
        }
        return new ProjectData(
                project,
                items,
                components,
                lore,
                array(export, "attributes"),
                fonts,
                glyphRows,
                sounds,
                sources,
                array(export, "expressionFunctions"),
                array(export, "expressions"),
                array(export, "assets"),
                array(export, "advancements"));
    }

    private static List<JsonNode> array(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            throw unavailable("the content export has no " + field + " list");
        }
        List<JsonNode> entries = new ArrayList<>();
        value.forEach(entries::add);
        return entries;
    }

    private static ReleaseRejectedException unavailable(String message) {
        return new ReleaseRejectedException(SOURCE_UNAVAILABLE, "backend: " + message);
    }
}
