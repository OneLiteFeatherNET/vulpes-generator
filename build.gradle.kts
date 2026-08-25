plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.micronaut.aot)
    jacoco
    `maven-publish`
    id("org.openapi.generator") version "7.25.0"
}

// group comes from gradle.properties. The version lives here, on the marker line below,
// as the single source of truth release-please rewrites. An explicit -Pversion=... still
// wins, so CI can (re)build the artifacts for a given version.
val releaseVersion = "0.2.0" // x-release-please-version
version = (findProperty("version") as? String)?.takeUnless { it == "unspecified" } ?: releaseVersion

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

    // Management endpoints + Micrometer's Prometheus registry. The Helm chart
    // probes /health and scrapes /prometheus, so both have to exist in every
    // environment the chart deploys to -- see charts/vulpes-generator.
    implementation(mn.micronaut.management)
    implementation(mn.micronaut.micrometer.core)
    implementation(mn.micronaut.micrometer.registry.prometheus)

    // SQL (JPA / read-only). Postgres, because that is where the Vulpes data
    // actually lives: the backend was migrated to the shared CNPG cluster and
    // no MariaDB `vulpes` database exists any more. The MariaDB driver stays on
    // the classpath so an existing local docker-compose setup keeps working --
    // which driver is used follows from the JDBC URL, not from what is present.
    implementation(mn.micronaut.jdbc.hikari)
    implementation(mn.micronaut.hibernate.jpa)
    implementation(mn.micronaut.data.hibernate.jpa)
    implementation(mn.micronaut.data.tx.hibernate)
    implementation(mn.mariadb.java.client)
    implementation(mn.postgresql)
    implementation(mn.micronaut.data.jpa)

    // Health endpoints and the Prometheus scrape target. The Helm chart in
    // charts/ already probes /health and points a ServiceMonitor at
    // /prometheus; without these the probes 404 and the pod never turns ready.
    implementation(mn.micronaut.management)
    implementation(mn.micronaut.micrometer.core)
    implementation(mn.micronaut.micrometer.registry.prometheus)

    // Jackson
    implementation(mn.jackson.core)
    implementation(mn.micronaut.serde.jackson)
    implementation(mn.jackson.databind)

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
    mainClass = "net.onelitefeather.vulpes.generator.VulpesGenerator"
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
        // Only the thin application jar is published. The fat runner jars and the
        // full distribution tar/zip bundle every dependency and exceed the Maven
        // repository's upload size limit (HTTP 413). The runnable artifact is the
        // Docker image, not a Maven artifact.
        artifact(project.tasks.jar)

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
