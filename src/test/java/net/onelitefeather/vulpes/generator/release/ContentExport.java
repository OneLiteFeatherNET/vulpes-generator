package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.JSON;

/**
 * Builds the content export of a project from its mapper input, in the shape {@code GET /project/{id}/content}
 * answers: every child list nested in its parent, lists as arrays. Used by the fake backend, so that tests can
 * describe a project with {@link ProjectFixture}.
 *
 * @author TheMeinerLP
 */
final class ContentExport {

    private ContentExport() {
    }

    /**
     * Nests the entities of one project into its content export.
     *
     * @param data the project data
     * @return the export object
     */
    static ObjectNode of(ProjectData data) {
        ObjectNode export = JSON.createObjectNode();
        export.put("revision", "test-revision");
        export.set("project", data.project());
        export.set("items", nest(nest(data.items(), "components", data.components()), "lore", data.lore()));
        export.set("attributes", array(data.attributes()));
        export.set("fonts", nest(data.fonts(), "chars", data.glyphRows()));
        export.set("sounds", nest(data.sounds(), "sources", data.sources()));
        export.set("expressionFunctions", array(data.expressionFunctions()));
        export.set("expressions", array(data.expressions()));
        export.set("assets", array(data.assets()));
        export.set("advancements", array(data.advancements()));
        export.set("dimensions", JSON.createArrayNode());
        export.set("packs", JSON.createArrayNode());
        return export;
    }

    /** Copies every parent and puts its children, looked up by parent id, into the given field. */
    private static ArrayNode nest(Iterable<JsonNode> parents, String field, Map<String, List<JsonNode>> children) {
        ArrayNode result = JSON.createArrayNode();
        for (JsonNode parent : parents) {
            ObjectNode copy = parent.deepCopy();
            copy.set(field, array(children.getOrDefault(parent.get("id").asText(), List.of())));
            result.add(copy);
        }
        return result;
    }

    private static ArrayNode array(List<JsonNode> entries) {
        ArrayNode array = JSON.createArrayNode();
        entries.forEach(array::add);
        return array;
    }
}
