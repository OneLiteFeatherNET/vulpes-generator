package net.onelitefeather.vulpes.generator.release;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads {@code maven-metadata.xml} of a project in a repository. A document that is not well-formed XML is a refusal
 * of the repository, never an empty list.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MavenMetadata {

    private MavenMetadata() {
    }

    /**
     * Returns the versions listed in the metadata of a project, in the order of the document.
     *
     * @param xml    the bytes of {@code maven-metadata.xml}
     * @param source the repository, named in the refusal, for example {@code the releases repository <url>}
     * @return the listed versions, empty when the document lists none
     * @throws ReleaseRejectedException with {@code REPOSITORY_UNAVAILABLE} when the document cannot be read
     */
    public static List<String> versions(byte[] xml, String source) {
        Element versioning = versioning(parse(xml, source), source);
        List<String> versions = new ArrayList<>();
        for (Element versionsElement : children(versioning, "versions")) {
            for (Element version : children(versionsElement, "version")) {
                versions.add(version.getTextContent().trim());
            }
        }
        return versions;
    }

    /**
     * Returns the timestamped version of the jar of a snapshot, for example {@code 1.1.0-20261107.221202-3}. Maven
     * Resolver deploys the file of a snapshot under this version.
     *
     * @param xml    the bytes of the version-level {@code maven-metadata.xml} of a snapshot
     * @param source the repository, named in the refusal
     * @return the timestamped version of the jar, or empty when the metadata names none
     * @throws ReleaseRejectedException with {@code REPOSITORY_UNAVAILABLE} when the document cannot be read
     */
    public static Optional<String> timestampedJarVersion(byte[] xml, String source) {
        Element versioning = versioning(parse(xml, source), source);
        for (Element snapshotVersions : children(versioning, "snapshotVersions")) {
            for (Element snapshotVersion : children(snapshotVersions, "snapshotVersion")) {
                boolean jar = children(snapshotVersion, "extension").stream()
                        .anyMatch(extension -> extension.getTextContent().trim().equals("jar"));
                boolean classified = !children(snapshotVersion, "classifier").isEmpty();
                if (jar && !classified) {
                    return children(snapshotVersion, "value").stream()
                            .map(value -> value.getTextContent().trim())
                            .findFirst();
                }
            }
        }
        return Optional.empty();
    }

    private static Document parse(byte[] xml, String source) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw unreadable(source, e);
        }
    }

    private static Element versioning(Document document, String source) {
        Element root = document.getDocumentElement();
        if (root == null || !"metadata".equals(root.getTagName())) {
            throw unreadable(source, new IllegalArgumentException("the document has no metadata root"));
        }
        List<Element> versioning = children(root, "versioning");
        return versioning.isEmpty() ? root : versioning.get(0);
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && name.equals(element.getTagName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static ReleaseRejectedException unreadable(String source, Exception cause) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE,
                "the metadata of " + source + " cannot be read: " + cause.getMessage());
    }
}
