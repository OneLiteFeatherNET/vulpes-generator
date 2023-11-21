plugins {
    java
    `maven-publish`
}

val vulpesGroupId: String by project
val vulpesVersion: String by project
val vulpesBaseUrl: String by project

group = vulpesGroupId
version = vulpesVersion

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")
    implementation("com.github.Minestom:Minestom:-SNAPSHOT")
}

publishing {
    publications {
        if (System.getenv().containsKey("CI")) {
            repositories {
                maven {
                    name = "GitLab"
                    url = uri(vulpesBaseUrl)
                    credentials(HttpHeaderCredentials::class.java) {
                        name = "Deploy-Token"
                        value = System.getenv("PUBLISH_TOKEN")
                    }
                    authentication {
                        create<HttpHeaderAuthentication>("header")
                    }
                }
            }
        }

        create<MavenPublication>("maven") {
            artifactId  = "vulpes"
            groupId = vulpesGroupId
            version = vulpesVersion

            from(components["java"])
        }
    }
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

//TODO:
//JUNIT
//"Database"