package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mutable builder of the backend data of one project. Tests add exactly the entities they need, with JSON in the
 * shape the backend REST endpoints return, and {@link #build()} turns them into the mapper input.
 *
 * @author TheMeinerLP
 */
final class ProjectFixture {

    static final ObjectMapper JSON = new ObjectMapper();

    private final JsonNode project;
    private final List<JsonNode> items = new ArrayList<>();
    private final Map<String, List<JsonNode>> components = new LinkedHashMap<>();
    private final Map<String, List<JsonNode>> lore = new LinkedHashMap<>();
    private final List<JsonNode> attributes = new ArrayList<>();
    private final List<JsonNode> fonts = new ArrayList<>();
    private final Map<String, List<JsonNode>> glyphRows = new LinkedHashMap<>();
    private final List<JsonNode> sounds = new ArrayList<>();
    private final Map<String, List<JsonNode>> sources = new LinkedHashMap<>();
    private final List<JsonNode> expressionFunctions = new ArrayList<>();
    private final List<JsonNode> expressions = new ArrayList<>();
    private final List<JsonNode> assets = new ArrayList<>();
    private final List<JsonNode> advancements = new ArrayList<>();

    /**
     * Creates a project with the given id, key and display name.
     *
     * @param id          the project id
     * @param key         the project key, which is also its namespace
     * @param displayName the display name, which becomes the pack description
     */
    ProjectFixture(UUID id, String key, String displayName) {
        this(obj("id", id.toString(), "key", key, "displayName", displayName));
    }

    /**
     * Creates a project from a captured project object.
     *
     * @param project the project object as the backend returned it
     */
    ProjectFixture(JsonNode project) {
        this.project = project;
    }

    /**
     * Returns the project fixture that carries the given key and display name.
     *
     * @param id          the project id
     * @param key         the project key
     * @param displayName the display name
     * @return a fresh fixture without entities
     */
    static ProjectFixture project(UUID id, String key, String displayName) {
        return new ProjectFixture(id, key, displayName);
    }

    /**
     * Builds an object node from alternating field names and values; strings, numbers, booleans and nulls are supported.
     *
     * @param fields the field names and values
     * @return the object node
     */
    static ObjectNode obj(Object... fields) {
        ObjectNode node = JSON.createObjectNode();
        for (int i = 0; i < fields.length; i += 2) {
            String name = (String) fields[i];
            Object value = fields[i + 1];
            switch (value) {
                case null -> node.putNull(name);
                case String s -> node.put(name, s);
                case Integer n -> node.put(name, n);
                case Double d -> node.put(name, d);
                case Boolean b -> node.put(name, b);
                case JsonNode n -> node.set(name, n);
                default -> throw new IllegalArgumentException("unsupported fixture value for " + name);
            }
        }
        return node;
    }

    /**
     * Parses JSON text into a node, for values that are JSON themselves.
     *
     * @param text the JSON text
     * @return the node
     */
    static JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("fixture JSON is invalid: " + text, e);
        }
    }

    /**
     * Returns a deterministic UUID for a small number, so that fixtures are readable.
     *
     * @param n the number
     * @return the UUID text
     */
    static String id(int n) {
        return String.format("00000000-0000-4000-8000-%012d", n);
    }

    JsonNode project() {
        return project;
    }

    List<JsonNode> items() {
        return items;
    }

    /** The component entries of one item, mutable. */
    List<JsonNode> components(String itemId) {
        return components.computeIfAbsent(itemId, id -> new ArrayList<>());
    }

    /** The lore lines of one item, mutable. */
    List<JsonNode> lore(String itemId) {
        return lore.computeIfAbsent(itemId, id -> new ArrayList<>());
    }

    List<JsonNode> attributes() {
        return attributes;
    }

    List<JsonNode> fonts() {
        return fonts;
    }

    /** The glyph rows of one font, mutable. */
    List<JsonNode> glyphRows(String fontId) {
        return glyphRows.computeIfAbsent(fontId, id -> new ArrayList<>());
    }

    List<JsonNode> sounds() {
        return sounds;
    }

    /** The sound sources of one sound event, mutable. */
    List<JsonNode> sources(String soundId) {
        return sources.computeIfAbsent(soundId, id -> new ArrayList<>());
    }

    List<JsonNode> expressionFunctions() {
        return expressionFunctions;
    }

    List<JsonNode> expressions() {
        return expressions;
    }

    List<JsonNode> assets() {
        return assets;
    }

    List<JsonNode> advancements() {
        return advancements;
    }

    /**
     * Freezes the fixture into the mapper input.
     *
     * @return the project data
     */
    ProjectData build() {
        return new ProjectData(project, items, components, lore, attributes, fonts, glyphRows, sounds, sources,
                expressionFunctions, expressions, assets, advancements);
    }
}
