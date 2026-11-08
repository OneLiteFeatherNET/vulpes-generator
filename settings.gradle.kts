rootProject.name = "vulpes-generator"
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("io.micronaut.platform.catalog") version "5.0.2"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "OneLiteFeatherRepository"
            url = uri("https://repo.onelitefeather.dev/onelitefeather")
            if (System.getenv("CI") != null) {
                credentials {
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            } else {
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
    versionCatalogs {
        create("libs") {
            version("micronaut", "5.0.2")
            version("vulpes.base", "0.6.0")
            version("vulpes.codegen", "0.4.1")
            version("vulpes.format", "0.4.1")
            version("mycelium", "1.8.9")
            version("jetbrains.annotation", "26.1.0")
            version("jgit", "7.8.1.202611041320-r")
            version("logstash.logback.encoder", "9.0")
            version("janino", "3.1.12")
            version("opentelemetry.instrumentation.alpha", "2.20.1-alpha")
            version("maven.resolver", "2.0.25")

            library("mycelium.bom", "net.onelitefeather", "mycelium-bom").versionRef("mycelium")

            // The code generator of the Vulpes Gradle plugin, consumed as a library (publish-codegen-library).
            library("vulpes.codegen", "net.onelitefeather.vulpes", "vulpes-codegen").versionRef("vulpes.codegen")
            library("vulpes.format", "net.onelitefeather.vulpes", "vulpes-format").versionRef("vulpes.format")
            // The base the code generator targets (vulpes-codegen/versions.properties); used by the integration tests only.
            library("vulpes.base", "net.onelitefeather.vulpes", "vulpes-base").versionRef("vulpes.base")
            library("jetbrains.annotation", "org.jetbrains", "annotations").versionRef("jetbrains.annotation")
            library("minestom","net.minestom", "minestom").withoutVersion()

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").versionRef("jgit")

            // Observability — JSON logging + OpenTelemetry (see build.gradle.kts).
            // OTLP exporter version is managed by the Micronaut platform BOM.
            library("logstash.logback.encoder", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash.logback.encoder")
            library("janino", "org.codehaus.janino", "janino").versionRef("janino")
            library("opentelemetry.exporter.otlp", "io.opentelemetry", "opentelemetry-exporter-otlp").withoutVersion()
            library("opentelemetry.logback.mdc", "io.opentelemetry.instrumentation", "opentelemetry-logback-mdc-1.0").versionRef("opentelemetry.instrumentation.alpha")

            // Publishing content data JARs to Reposilite (see release package).
            library("maven.resolver.supplier", "org.apache.maven.resolver", "maven-resolver-supplier-mvn3").versionRef("maven.resolver")
            library("maven.resolver.transport.jdk", "org.apache.maven.resolver", "maven-resolver-transport-jdk").versionRef("maven.resolver")

            plugin("micronaut.application", "io.micronaut.application").versionRef("micronaut")
            plugin("micronaut.aot", "io.micronaut.aot").versionRef("micronaut")
        }
    }
}
