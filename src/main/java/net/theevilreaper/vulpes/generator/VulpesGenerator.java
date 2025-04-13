package net.theevilreaper.vulpes.generator;

import io.micronaut.context.annotation.Import;
import io.micronaut.runtime.Micronaut;

/**
 * Main class to start the application.
 */
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
