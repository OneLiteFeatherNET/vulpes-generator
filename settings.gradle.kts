rootProject.name = "vulpes-generator"
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("io.micronaut.platform.catalog") version "5.0.1"
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
            version("micronaut", "5.0.1")
            version("vulpes.base", "0.5.1")
            version("vulpes.model", "1.7.1")
            version("mycelium", "1.7.2")
            version("jetbrains.annotation", "26.1.0")
            version("javapoet", "1.13.0")
            version("jgit", "7.7.0.202606012155-r")
            version("guava", "33.6.0-jre")
            version("commons.io", "2.22.0")
            version("commons.compress", "1.28.0")
            version("logstash.logback.encoder", "9.0")
            version("janino", "3.1.12")
            version("opentelemetry.instrumentation.alpha", "2.20.1-alpha")

            library("mycelium.bom", "net.onelitefeather", "mycelium-bom").versionRef("mycelium")

            library("vulpes.model", "net.onelitefeather", "vulpes-model").versionRef("vulpes.model")
            //library("vulpes.base", "net.theevilreaper.vulpes.base", "vulpes").versionRef("vulpes.base")
            library("jetbrains.annotation", "org.jetbrains", "annotations").versionRef("jetbrains.annotation")
            library("javapoet", "com.squareup", "javapoet").versionRef("javapoet")
            library("minestom","net.minestom", "minestom").withoutVersion()

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").versionRef("jgit")
            library("guava", "com.google.guava", "guava").versionRef("guava")
            library("commons.io", "commons-io", "commons-io").versionRef("commons.io")
            library("commons.compress", "org.apache.commons", "commons-compress").versionRef("commons.compress")

            // Observability — JSON logging + OpenTelemetry (see build.gradle.kts).
            // OTLP exporter version is managed by the Micronaut platform BOM.
            library("logstash.logback.encoder", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash.logback.encoder")
            library("janino", "org.codehaus.janino", "janino").versionRef("janino")
            library("opentelemetry.exporter.otlp", "io.opentelemetry", "opentelemetry-exporter-otlp").withoutVersion()
            library("opentelemetry.logback.mdc", "io.opentelemetry.instrumentation", "opentelemetry-logback-mdc-1.0").versionRef("opentelemetry.instrumentation.alpha")

            plugin("micronaut.application", "io.micronaut.application").versionRef("micronaut")
            plugin("micronaut.aot", "io.micronaut.aot").versionRef("micronaut")

            bundle("vulpes", listOf("vulpes.model", /*"vulpes.base"*/))
            bundle("apache.commons", listOf("commons-io", "commons-io"))
        }
    }
}
