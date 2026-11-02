package net.onelitefeather.vulpes.generator.release;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Validates snapshot bytes against the format 1 contract.
 * <p>
 * The schema is a copy of {@code snapshot-1.schema.json} from the {@code vulpes-gradle-plugin} repository,
 * {@code vulpes-format/src/main/resources/net/onelitefeather/vulpes/format/snapshot-1.schema.json}. It is copied
 * into {@code src/test/resources/release} so that the tests do not depend on another checkout.
 *
 * @author TheMeinerLP
 */
final class SnapshotSchema {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Schema SCHEMA = load();

    private SnapshotSchema() {
    }

    /**
     * Returns every violation of the schema, an empty list when the snapshot is valid.
     *
     * @param snapshot the snapshot bytes
     * @return the violation messages with their instance locations
     */
    static List<String> violations(byte[] snapshot) {
        JsonNode document = JSON.readTree(snapshot);
        return SCHEMA.validate(document).stream().map(SnapshotSchema::describe).toList();
    }

    private static String describe(Error error) {
        return error.getInstanceLocation() + ": " + error.getMessage();
    }

    private static Schema load() {
        try (InputStream schema = SnapshotSchema.class.getResourceAsStream("/release/snapshot-1.schema.json")) {
            if (schema == null) {
                throw new IllegalStateException("snapshot-1.schema.json is missing from the test resources");
            }
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
