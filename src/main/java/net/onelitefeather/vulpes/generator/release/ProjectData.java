package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * The entities of one project as the backend content export holds them, before any mapping.
 * Sub-resources are keyed by the id of their parent: components and lore by item id, glyph rows by font id,
 * sound sources by sound id. A parent without an entry has no sub-resources.
 *
 * @param project            the project object
 * @param items              the items of the project
 * @param components         component entries by item id
 * @param lore               lore lines by item id
 * @param attributes         the attributes of the project
 * @param fonts              the fonts of the project
 * @param glyphRows          glyph rows by font id
 * @param sounds             the sound events of the project
 * @param sources            sound sources by sound id
 * @param expressionFunctions the expression functions of the project
 * @param expressions        the expressions of the project
 * @param assets             the asset metadata of the project
 * @param advancements       the advancements of the project, which the release does not support yet
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
record ProjectData(
        JsonNode project,
        List<JsonNode> items,
        Map<String, List<JsonNode>> components,
        Map<String, List<JsonNode>> lore,
        List<JsonNode> attributes,
        List<JsonNode> fonts,
        Map<String, List<JsonNode>> glyphRows,
        List<JsonNode> sounds,
        Map<String, List<JsonNode>> sources,
        List<JsonNode> expressionFunctions,
        List<JsonNode> expressions,
        List<JsonNode> assets,
        List<JsonNode> advancements
) {
}
