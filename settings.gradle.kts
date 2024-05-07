rootProject.name = "vulpes-spring-generator"

dependencyResolutionManagement {
    if (System.getenv("CI") != null) {
        repositoriesMode = RepositoriesMode.PREFER_SETTINGS
        repositories {
            maven("https://repo.htl-md.schule/repository/Gitlab-Runner/")
            maven {
                val groupdId = 28 // Gitlab Group
                val ciApiv4Url = System.getenv("CI_API_V4_URL")
                url = uri("$ciApiv4Url/groups/$groupdId/-/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class.java) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.23")

            library("vulpes.api", "net.theevilreaper.vulpes.api", "vulpes-spring-api").version("0.0.1+1f9ff402")
            library("vulpes.base", "dev.themeinerlp", "vulpes-base").version("1.0-SNAPSHOT+95bd27ce")
            library("dartpoet", "dev.themeinerlp", "dartpoet").version("0.0.1-SNAPSHOT")
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("24.1.0")
            library("javapoet", "com.squareup", "javapoet").version("1.13.0")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").version("3.1.8")
            library("microtus", "net.onelitefeather.microtus", "Minestom").version("1.3.1")

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").version("6.9.0.202403050737-r")
            // We need to use the RC version of Gitlab4j api because Spring Boot 3 requires Jakarta as a dependency which is only supported in the RC version
            library("gitlab4j", "org.gitlab4j", "gitlab4j-api").version("6.0.0-rc.4")
            library("guava", "com.google.guava", "guava").version("33.1.0-jre")
            library("commons.io", "commons-io", "commons-io").version("2.16.1")
            library("commons.compress", "org.apache.commons", "commons-compress").version("1.26.1")
            library("jackson", "com.fasterxml.jackson.module", "jackson-module-kotlin").version("2.17.1")


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
            plugin("kotlin.spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")
            plugin("spring", "org.springframework.boot").version("3.2.5")
            plugin("spring.dependency", "io.spring.dependency-management").version("1.1.4")

            bundle(
                "spring",
                listOf(
                    "spring.starter.web",
                    "spring.starter.data.mongodb",
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
