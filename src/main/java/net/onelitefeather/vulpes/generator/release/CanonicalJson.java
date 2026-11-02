package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Writes a JSON tree in the canonical form of snapshots: object keys sorted at every level, no insignificant
 * whitespace, UTF-8 with non-ASCII characters written as they are. Array order is kept, so callers sort the arrays.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
final class CanonicalJson {

    private static final JsonFactory FACTORY = new JsonFactory();

    private CanonicalJson() {
    }

    /**
     * Encodes a tree canonically.
     *
     * @param root the tree to encode
     * @return the UTF-8 bytes without a trailing newline
     */
    static byte[] write(JsonNode root) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator generator = FACTORY.createGenerator(out, JsonEncoding.UTF8)) {
            write(generator, root);
        } catch (IOException e) {
            throw new UncheckedIOException("canonical JSON could not be written to memory", e);
        }
        return out.toByteArray();
    }

    private static void write(JsonGenerator generator, JsonNode node) throws IOException {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            generator.writeStartObject();
            for (String name : names) {
                generator.writeFieldName(name);
                write(generator, node.get(name));
            }
            generator.writeEndObject();
        } else if (node.isArray()) {
            generator.writeStartArray();
            for (JsonNode element : node) {
                write(generator, element);
            }
            generator.writeEndArray();
        } else if (node.isTextual()) {
            generator.writeString(node.textValue());
        } else if (node.isBoolean()) {
            generator.writeBoolean(node.booleanValue());
        } else if (node.isNumber()) {
            generator.writeNumber(node.numberValue().toString());
        } else if (node.isNull() || node.isMissingNode()) {
            generator.writeNull();
        } else {
            throw new IllegalArgumentException("node type cannot be written canonically: " + node.getNodeType());
        }
    }
}
