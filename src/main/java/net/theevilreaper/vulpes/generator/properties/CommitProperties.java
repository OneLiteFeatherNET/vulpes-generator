package net.theevilreaper.vulpes.generator.properties;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration properties for commit-related information.
 * <p>
 * This class is automatically populated from the application's configuration
 * (e.g., `application.yml`) under the `commit` namespace.
 * </p>
 *
 * <p>
 * If no values are provided in the configuration, default values are assigned:
 * </p>
 * <ul>
 *     <li>Author: "Vulpes"</li>
 *     <li>Mail: "vulpes@onelitefeather.com"</li>
 *     <li>Message: "Default commit message"</li>
 * </ul>
 *
 * Example configuration in <code>application.yml</code>:
 * <pre>
 * commit:
 *   author: "John Doe"
 *   mail: "john.doe@example.com"
 *   message: "Initial commit"
 * </pre>
 */
@ConfigurationProperties("commit")
public record CommitProperties(
        String author,
        String mail,
        String message
) {
    /**
     * Constructor that ensures default values are applied if no configuration is provided.
     *
     * @param author The commit author's name (defaults to "Vulpes" if null).
     * @param mail The commit author's email (defaults to "vulpes@onelitefeather.com" if null).
     * @param message The default commit message (defaults to "Default commit message" if null).
     */
    public CommitProperties {
        if (mail == null) {
            mail = "vulpes@onelitefeather.com";
        }
        if (author == null) {
            author = "Vulpes";
        }
        if (message == null) {
            message = "Default commit message";
        }
    }
}

