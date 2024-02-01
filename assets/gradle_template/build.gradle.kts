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

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(libs.jetbrains.annotation)
    implementation(libs.microtus)
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
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

//TODO:
//JUNIT
//"Database"