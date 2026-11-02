package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.CONTENT_CHANGED;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.UNKNOWN_PROJECT;

/**
 * Assembles the format 1 snapshot of a project from the REST endpoints of the backend, instead of a frozen snapshot
 * endpoint that the backend does not have.
 * <p>
 * The project is read twice in a row and the snapshot is used only when both results are byte-identical. A project
 * that changed in between is read again, up to {@value #ATTEMPTS} times in all; then the release is refused with
 * {@link ReleaseRejectedException.Reason#CONTENT_CHANGED}. The check narrows the window for a mixed state but does not
 * close it: an edit that is reverted between the two reads goes unnoticed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BackendApiReleaseSnapshotSource implements ReleaseSnapshotSource {

    /** The number of attempts, each made of two reads in a row. */
    static final int ATTEMPTS = 3;

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

    /**
     * Assembles the snapshot of a project, with its key.
     *
     * @param projectId the id of the project
     * @return the stable snapshot
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#UNKNOWN_PROJECT},
     *                                  {@link ReleaseRejectedException.Reason#SOURCE_UNAVAILABLE},
     *                                  {@link ReleaseRejectedException.Reason#UNSUPPORTED_CONTENT} or
     *                                  {@link ReleaseRejectedException.Reason#CONTENT_CHANGED}
     */
    public AssembledSnapshot assemble(UUID projectId) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            AssembledSnapshot first = readOnce(projectId);
            AssembledSnapshot second = readOnce(projectId);
            if (Arrays.equals(first.bytes(), second.bytes())) {
                return second;
            }
        }
        throw new ReleaseRejectedException(CONTENT_CHANGED,
                "content of project " + projectId + " changed during the release, try again");
    }

    private AssembledSnapshot readOnce(UUID projectId) {
        String project = "/project/" + projectId;
        JsonNode projectNode = backend.find(project).orElseThrow(() -> new ReleaseRejectedException(UNKNOWN_PROJECT,
                "backend does not know project " + projectId));
        List<JsonNode> items = backend.list(project + "/item");
        List<JsonNode> fonts = backend.list(project + "/font");
        List<JsonNode> sounds = backend.list(project + "/sound");
        ProjectData data = new ProjectData(
                projectNode,
                items,
                perParent(items, id -> "/item/" + id + "/components"),
                perParent(items, id -> "/item/" + id + "/lore"),
                backend.list(project + "/attribute"),
                fonts,
                perParent(fonts, id -> "/font/chars/" + id),
                sounds,
                perParent(sounds, id -> "/sound/" + id + "/sources"),
                backend.list(project + "/expression-function"),
                backend.list(project + "/expression"),
                backend.list(project + "/asset"),
                backend.list(project + "/advancement"));
        return mapper.map(data);
    }

    private Map<String, List<JsonNode>> perParent(List<JsonNode> parents, Function<String, String> path) {
        Map<String, List<JsonNode>> result = new LinkedHashMap<>();
        for (JsonNode parent : parents) {
            String id = parent.get("id").asText();
            result.put(id, backend.list(path.apply(id)));
        }
        return result;
    }
}
