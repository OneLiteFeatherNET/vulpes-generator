package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes {@code META-INF/vulpes/release.json} in release format 2, which adds the {@code base} object.
 * The JSON is written by hand so that the key order, the absence of whitespace and the escaping
 * are fixed by this class and not by a serializer.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class ReleaseJsonWriter {

    /**
     * Encodes the release metadata as canonical UTF-8 JSON.
     * {@code createdAt} and {@code createdBy} come from the request; {@code generator} is the given generator version.
     * The {@code base} object names the {@code vulpes-base} repository, branch and full commit of the request.
     *
     * @param request         the validated release request
     * @param base            the verified {@code vulpes-base} checkout
     * @param generatorVersion the version of this generator
     * @return the UTF-8 encoded {@code release.json} content
     */
    public byte[] write(ReleaseRequest request, ReleaseBase base, String generatorVersion) {
        return write(request, base, generatorVersion, List.of());
    }

    /**
     * Encodes the release metadata with the packs that the release uploaded, as canonical UTF-8 JSON.
     *
     * @param request          the validated release request
     * @param base             the verified {@code vulpes-base} checkout
     * @param generatorVersion the version of this generator
     * @param packs            the resource packs of the release, empty when none is uploaded
     * @return the UTF-8 encoded {@code release.json} content
     */
    public byte[] write(ReleaseRequest request, ReleaseBase base, String generatorVersion, List<ReleasePack> packs) {
        StringBuilder json = new StringBuilder(256);
        json.append("{\"base\":{\"branch\":");
        appendString(json, base.branch());
        json.append(",\"commit\":");
        appendString(json, base.commit());
        json.append(",\"repository\":");
        appendString(json, base.repository());
        json.append("},\"format\":2,\"packs\":[");
        for (int i = 0; i < packs.size(); i++) {
            ReleasePack pack = packs.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":");
            appendString(json, pack.id());
            json.append(",\"resolution\":").append(pack.resolution());
            json.append(",\"sha1\":");
            appendString(json, pack.sha1());
            json.append(",\"size\":").append(pack.size());
            json.append(",\"url\":");
            appendString(json, pack.url());
            json.append('}');
        }
        json.append("],\"project\":");
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
