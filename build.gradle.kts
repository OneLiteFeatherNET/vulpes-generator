plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.micronaut.aot)
    jacoco
    `maven-publish`
    id("org.openapi.generator") version "7.23.0"
}

// group comes from gradle.properties; strip the release-please annotation comment.
version = (version as String).substringBefore('#').trim()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // Annotation processing
    annotationProcessor(mn.micronaut.serde.processor)
    annotationProcessor(mn.micronaut.data.processor)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(mn.micronaut.openapi)
    annotationProcessor(mn.micronaut.validation.processor)

    compileOnly(mn.micronaut.openapi.annotations)

    // Micronaut runtime
    implementation(mn.micronaut.runtime)
    implementation(mn.micronaut.http.client)

    // SQL (JPA / read-only)
    implementation(mn.micronaut.jdbc.hikari)
    implementation(mn.micronaut.hibernate.jpa)
    implementation(mn.micronaut.data.hibernate.jpa)
    implementation(mn.micronaut.data.tx.hibernate)
    implementation(mn.mariadb.java.client)
    implementation(mn.micronaut.data.jpa)

    // Jackson
    implementation(mn.jackson.core)
    implementation(mn.micronaut.serde.jackson)
    implementation(mn.jackson.databind)
    implementation(mn.jackson.datatype.jsr310)

    // Logging
    implementation(mn.logback.classic)
    // Distributed tracing (OpenTelemetry). Spans/export are only active when
    // OTEL_TRACES_EXPORTER=otlp is set (prod/Docker) — see application.yml.
    implementation(mn.micronaut.tracing.opentelemetry.http)
    implementation(mn.micronaut.tracing.opentelemetry.jdbc)
    implementation(libs.opentelemetry.exporter.otlp)
    // Structured JSON logging for Grafana Loki + trace/log correlation.
    // logstash encoder renders JSON; the OTel MDC appender injects trace_id/span_id.
    implementation(libs.logstash.logback.encoder)
    implementation(libs.opentelemetry.logback.mdc)
    // Enables the <if>/<then>/<else> conditional in logback.xml.
    runtimeOnly(libs.janino)

    implementation(mn.micronaut.openapi)
    implementation(mn.swagger.core)

    // Misc
    implementation(platform(libs.mycelium.bom))
    implementation(mn.micronaut.http.client)
    implementation(libs.bundles.vulpes)
    implementation(libs.jetbrains.annotation)
    implementation(libs.javapoet)
    implementation(libs.minestom)
    implementation(libs.jgit)
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(libs.commons.compress)

    runtimeOnly(mn.snakeyaml)

    // Tests
    testImplementation(mn.micronaut.test.junit5)
    testImplementation(mn.mockito.core)
    testImplementation(mn.mockito.junit.jupiter)
    testImplementation(mn.junit.jupiter.api)
    testImplementation(mn.junit.jupiter.params)
    testRuntimeOnly(mn.junit.jupiter.engine)
}

tasks {
    jacocoTestReport {
        dependsOn(project.tasks.test)
        reports {
            xml.required.set(true)
        }
    }

    this.openApiGenerate {
        dependsOn("compileJava")
    }

    test {
        finalizedBy(project.tasks.jacocoTestReport)
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.release = 25
        options.forkOptions.jvmArgs = listOf("-Dmicronaut.openapi.views.spec=rapidoc.enabled=true,openapi-explorer.enabled=true,swagger-ui.enabled=true,swagger-ui.theme=flattop")
    }
}

application {
    mainClass = "net.theevilreaper.vulpes.generator.VulpesGenerator"
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations(
            "net.onelitefeather.vulpes.*"
        )
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = false
        deduceEnvironment = true
        optimizeNetty = true
        // Keep logback.xml parsed at runtime so its <if> JSON/plain switch and the
        // OpenTelemetry MDC appender work (AOT replacement would inline a static config).
        replaceLogbackXml = false
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifact(project.tasks.optimizedJitJar)
        artifact(project.tasks.optimizedRunnerJitJar)
        artifact(project.tasks.runnerJar)
        artifact(project.tasks.jar)
        artifact(project.tasks.optimizedDistTar)
        artifact(project.tasks.optimizedDistZip)

        version = rootProject.version as String
        artifactId = "vulpes-generator"
        groupId = rootProject.group as String
        pom {
            name = "Vulpes Generator"
            description = "Code/asset generator for OneLiteFeather's Vulpes project."
            url = "https://github.com/OneLiteFeatherNET/vulpes-generator"
            licenses {
                license {
                    name = "AGPL-3.0"
                    url = "https://www.gnu.org/licenses/agpl-3.0.en.html"
                }
            }
            developers {
                developer {
                    id = "themeinerlp"
                    name = "Phillipp Glanz"
                    email = "p.glanz@madfix.me"
                }
                developer {
                    id = "theEvilReaper"
                    name = "Steffen Wonning"
                    email = "steffenwx@gmail.com"
                }
            }
            scm {
                connection = "scm:git:git://github.com:OneLiteFeatherNET/vulpes-generator.git"
                developerConnection = "scm:git:ssh://git@github.com:OneLiteFeatherNET/vulpes-generator.git"
                url = "https://github.com/OneLiteFeatherNET/vulpes-generator"
            }
        }
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    // Those credentials need to be set under "Settings -> Secrets -> Actions" in your repository
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            }

            name = "OneLiteFeatherRepository"
            val releasesRepoUrl = uri("https://repo.onelitefeather.dev/onelitefeather-releases")
            val snapshotsRepoUrl = uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
            url = if (version.toString().contains("BETA") || version.toString().contains("ALPHA") || version.toString().contains("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}
