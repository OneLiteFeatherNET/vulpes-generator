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
    } else {
        repositories {
            mavenLocal()
            mavenCentral()
            maven { url = uri("https://repo.spring.io/milestone") }
            maven { url = uri("https://repo.spring.io/snapshot") }
            maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            //maven("https://reposilite.worldseed.online/public")
            maven("https://jitpack.io")
            maven {
                val groupdId = 28 // Gitlab Group
                url = uri("https://gitlab.onelitefeather.dev/api/v4/groups/$groupdId/-/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class.java) {
                    name = "Private-Token"
                    val gitLabPrivateToken: String? by settings
                    value = gitLabPrivateToken
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
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
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("24.1.0")
            library("javapoet", "com.squareup", "javapoet").version("1.13.0")
            library("microtus", "net.onelitefeather.microtus", "Minestom").version("1.3.1")

            library("jgit", "org.eclipse.jgit", "org.eclipse.jgit").version("7.0.0.202409031743-r")
            library("gitlab4j", "org.gitlab4j", "gitlab4j-api").version("6.0.0-rc.9")
            library("guava", "com.google.guava", "guava").version("33.3.1-jre")
            library("commons.io", "commons-io", "commons-io").version("2.17.0")
            library("commons.compress", "org.apache.commons", "commons-compress").version("1.27.1")
            library("jackson", "com.fasterxml.jackson.module", "jackson-module-kotlin").version("2.18.3")

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
