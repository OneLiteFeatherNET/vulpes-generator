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
        maven {  }
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
            version("kotlin", "2.0.21")
            version("micronaut", "4.4.4")

            library("vulpes.api", "net.theevilreaper.vulpes.api", "vulpes-spring-api").version("1.0.0-SNAPSHOT")
            library("vulpes.base", "dev.themeinerlp", "vulpes-base").version("1.0-SNAPSHOT+95bd27ce")
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("26.0.2")
            library("javapoet", "com.squareup", "javapoet").version("1.13.0")
            library("microtus", "net.onelitefeather.microtus", "Minestom").version("1.3.1")

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").version("7.0.0.202409031743-r")
            library("gitlab4j", "org.gitlab4j", "gitlab4j-api").version("6.0.0-rc.9")
            library("guava", "com.google.guava", "guava").version("33.3.1-jre")
            library("commons.io", "commons-io", "commons-io").version("2.17.0")
            library("commons.compress", "org.apache.commons", "commons-compress").version("1.27.1")
            library("jackson", "com.fasterxml.jackson.module", "jackson-module-kotlin").version("2.18.3")

            //Spring
            library("spring.starter.web", "org.springframework.boot", "spring-boot-starter-web").withoutVersion()
            library(
                "spring.starter.data.mongodb",
                "org.springframework.boot",
                "spring-boot-starter-data-mongodb"
            ).withoutVersion()
            library(
                "spring.starter.webflux",
                "org.springframework.boot",
                "spring-boot-starter-webflux"
            ).withoutVersion()
            library("spring.starter.cache", "org.springframework.boot", "spring-boot-starter-cache").withoutVersion()
            library("spring.starter.log4j2", "org.springframework.boot", "spring-boot-starter-log4j2").withoutVersion()


            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("micronaut.application", "io.micronaut.application").versionRef("micronaut")
            plugin("micronaut.aot", "io.micronaut.aot").versionRef("micronaut")


            bundle(
                "vulpes",
                listOf(
                    "vulpes.api",
                    "vulpes.base"
                )
            )
        }
    }
}
