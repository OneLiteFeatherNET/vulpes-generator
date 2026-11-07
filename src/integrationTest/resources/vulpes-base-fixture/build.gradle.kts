// A minimal stand-in for vulpes-base: one class, no dependencies, the same Gradle wrapper as the generator.
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
