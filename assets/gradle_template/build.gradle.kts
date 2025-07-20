plugins {
    java
    `maven-publish`
}

val vulpesGroupId: String by project
val vulpesVersion: String by project
val vulpesBaseUrl: String by project

group = vulpesGroupId
version = vulpesVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(platform(libs.mycelium.bom))
    implementation(libs.adventure)
    implementation(libs.minestom)

    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

publishing {
    publishing {
        publications.create<MavenPublication>("maven") {
            from(components["java"])
        }

        repositories {
            maven {
                authentication {
                    credentials(PasswordCredentials::class) {
                        username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                        password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                    }
                }
                name = "OneLiteFeatherRepository"
                url = if (project.version.toString().contains("SNAPSHOT")) {
                    uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
                } else {
                    uri("https://repo.onelitefeather.dev/onelitefeather-releases")
                }
            }
        }
    }
}