package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Writes {@code META-INF/vulpes/release.json} in release format 1.
 * The JSON is written by hand so that the key order, the absence of whitespace and the escaping
 * are fixed by this class and not by a serializer.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class ReleaseJsonWriter {

    /**
     * Encodes the release metadata as canonical UTF-8 JSON.
     * {@code createdAt} and {@code createdBy} come from the request; {@code generator} is the given generator version.
     *
     * @param request        the validated release request
     * @param generatorVersion the version of this generator
     * @return the UTF-8 encoded {@code release.json} content
     */
    public byte[] write(ReleaseRequest request, String generatorVersion) {
        StringBuilder json = new StringBuilder(256);
        json.append("{\"format\":1,\"packs\":[],\"project\":");
        appendString(json, request.project());
        json.append(",\"projectId\":");
        appendString(json, request.projectId().toString());
        json.append(",\"provenance\":{\"createdAt\":");
        appendString(json, DateTimeFormatter.ISO_INSTANT.format(request.createdAt()));
        json.append(",\"createdBy\":");
        appendString(json, request.createdBy());
        json.append(",\"generator\":");
        appendString(json, generatorVersion);
        json.append("},\"revision\":");
        appendString(json, request.revision());
        json.append(",\"version\":");
        appendString(json, request.version());
        json.append('}');
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Appends a JSON string literal. Quotes and backslashes are escaped, other control characters
     * are written as {@code \\uXXXX}; everything else is written as is.
     */
    private static void appendString(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }
}
