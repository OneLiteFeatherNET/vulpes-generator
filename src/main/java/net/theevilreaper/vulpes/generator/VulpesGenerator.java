package net.theevilreaper.vulpes.generator;

import io.micronaut.context.annotation.Import;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Main class to start the application.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Vulpes Generator",
                version = "0.0.1",
                description = "Vulpes generator for generating java related code.",
                license = @License(name = "AGPL-3.0"),
                contact = @Contact(url = "https://onelitefeather.net", name = "Management", email = "admin@onelitefeather.net")
        ),
        tags = {
                @Tag(name = "generation", description = "Entity generation"),
        }
)
@Import(
        packages = {
                "net.theevilreaper.vulpes.*",
        },
        annotated = "*"
)
public class VulpesGenerator {

    /**
     * Main method to start the application.
     *
     * @param args the arguments passed to the application
     */
    public static void main(String[] args) {
        Micronaut.run(VulpesGenerator.class, args);
    }
}
