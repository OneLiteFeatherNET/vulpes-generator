rootProject.name = "vulpes-generator"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}

plugins {
    id("io.micronaut.platform.catalog") version "4.5.3"
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
            version("micronaut", "4.5.3")
            version("vulpes.base", "0.5.1")
            version("vulpes.model", "1.2.0")

            library("vulpes.model", "net.onelitefeather.vulpes", "vulpes-model").versionRef("vulpes.model")
            library("vulpes.base", "net.theevilreaper.vulpes.base", "vulpes").versionRef("vulpes.base")
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("26.0.2")
            library("javapoet", "com.squareup", "javapoet").version("1.13.0")
            library("microtus", "net.onelitefeather.microtus", "Microtus").version("1.5.1")

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").version("7.2.1.202505142326-r")
            library("gitlab4j", "org.gitlab4j", "gitlab4j-api").version("6.0.0")
            library("guava", "com.google.guava", "guava").version("33.4.8-jre")
            library("commons.io", "commons-io", "commons-io").version("2.19.0")
            library("commons.compress", "org.apache.commons", "commons-compress").version("1.27.1")

            plugin("micronaut.application", "io.micronaut.application").versionRef("micronaut")
            plugin("micronaut.aot", "io.micronaut.aot").versionRef("micronaut")

            bundle("vulpes", listOf("vulpes.model", "vulpes.base"))
        }
    }
}
