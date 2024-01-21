rootProject.name = "vulpes-spring-generator"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.22")

            library("vulpes.api", "net.theevilreaper.vulpes.api", "vulpes-spring-api").version("0.0.1+1f9ff402")
            library("vulpes.base", "dev.themeinerlp", "vulpes-base").version("1.0-SNAPSHOT+95bd27ce")
            library("dartpoet", "dev.themeinerlp", "dartpoet").version("0.0.1-SNAPSHOT")
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("24.1.0")
            library("javapoet", "com.squareup", "javapoet").version("1.13.0")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").version("3.1.8")
            library("microtus", "net.onelitefeather.microtus", "Minestom").version("1.2.1")

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").version("6.8.0.202311291450-r")
            library("gitlab4j", "org.gitlab4j", "gitlab4j-api").version("5.5.0")
            library("guava", "com.google.guava", "guava").version("33.0.0-jre")
            library("commons.io", "commons-io", "commons-io").version("2.15.1")
            library("commons.compress", "org.apache.commons", "commons-compress").version("1.25.0")
            library("jackson", "com.fasterxml.jackson.module", "jackson-module-kotlin").version("2.16.1")


            //Spring
            library("spring.starter.web", "org.springframework.boot", "spring-boot-starter-web").withoutVersion()
            library(
                "spring.starter.data.mongodb",
                "org.springframework.boot",
                "spring-boot-starter-data-mongodb"
            ).withoutVersion()
            library(
                "spring.starter.security",
                "org.springframework.boot",
                "spring-boot-starter-security"
            ).withoutVersion()
            library(
                "spring.starter.webflux",
                "org.springframework.boot",
                "spring-boot-starter-webflux"
            ).withoutVersion()
            library("spring.starter.cache", "org.springframework.boot", "spring-boot-starter-cache").withoutVersion()
            library("spring.starter.log4j2", "org.springframework.boot", "spring-boot-starter-log4j2").withoutVersion()


            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin.spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")
            plugin("spring", "org.springframework.boot").version("3.2.2")
            plugin("spring.dependency", "io.spring.dependency-management").version("1.1.4")

            bundle(
                "spring",
                listOf(
                    "spring.starter.web",
                    "spring.starter.data.mongodb",
                    "spring.starter.security",
                    "spring.starter.webflux",
                    "spring.starter.cache",
                    "spring.starter.log4j2"
                )
            )

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
