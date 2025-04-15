rootProject.name = "vulpes-generator"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}

plugins {
    id("io.micronaut.platform.catalog") version "4.4.4"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://reposilite.worldseed.online/public")
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
            version("micronaut", "4.4.4")
            version("vulpes.base", "0.5.0")
            version("vulpes.model", "1.0.1")

            library("vulpes.model", "net.theevilreaper.vulpes.api", "vulpes-model").versionRef("vulpes.model")
            library("vulpes.base", "net.thevilreaper.vulpes.base", "vulpes").versionRef("vulpes.base")
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("26.0.2")
            library("javapoet", "com.squareup", "javapoet").version("1.13.0")
            library("microtus", "net.onelitefeather.microtus", "Microtus").version("1.5.1")

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").version("7.0.0.202409031743-r")
            library("gitlab4j", "org.gitlab4j", "gitlab4j-api").version("6.0.0-rc.9")
            library("guava", "com.google.guava", "guava").version("33.3.1-jre")
            library("commons.io", "commons-io", "commons-io").version("2.17.0")
            library("commons.compress", "org.apache.commons", "commons-compress").version("1.27.1")

            plugin("micronaut.application", "io.micronaut.application").versionRef("micronaut")
            plugin("micronaut.aot", "io.micronaut.aot").versionRef("micronaut")

            bundle("vulpes", listOf("vulpes.model", "vulpes.base"))
        }
    }
}
