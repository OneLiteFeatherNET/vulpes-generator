plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.micronaut.aot)
    jacoco
}

group = "net.theevilreaper"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // Annotation processing
    annotationProcessor(mn.micronaut.serde.processor)
    annotationProcessor(mn.micronaut.data.processor)
    annotationProcessor(mn.micronaut.openapi)
    annotationProcessor(mn.micronaut.inject.java)

    compileOnly(mn.micronaut.openapi.annotations)

    // Micronaut runtime
    implementation(mn.micronaut.runtime)
    implementation(mn.micronaut.http.client)

    // SQL (JPA / read-only)
    implementation(mn.micronaut.jdbc.hikari)
    implementation(mn.micronaut.hibernate.jpa)
    implementation(mn.mariadb.java.client)
    implementation(mn.micronaut.data.jpa)

    // Jackson
    implementation(mn.jackson.core)
    implementation(mn.micronaut.serde.jackson)
    implementation(mn.jackson.databind)
    implementation(mn.jackson.datatype.jsr310)

    // Logging (empfohlen)
    implementation(mn.logback.classic)

    // Misc
    implementation(platform(libs.mycelium.bom))
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
    register<Zip>("gradleTemplateZip") {
        exclude("DS_Store")
        archiveFileName.set("gradle_template.zip")
        destinationDirectory.set(file("src/main/resources"))
        from("${rootDir}/assets/gradle_template")
    }

    register<Copy>("copyGitlabCiFile") {
        from("$rootDir/assets/gitlab/.gitlab-ci.yml")
        into("src/main/resources")
    }

    inspectRuntimeClasspath {
        dependsOn("processResources")
    }

    processResources {
        dependsOn("gradleTemplateZip")
        dependsOn("copyGitlabCiFile")
    }

    jacocoTestReport {
        dependsOn(project.tasks.test)
        reports {
            xml.required.set(true)
        }
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
    }
}

application {
    mainClass = "net.theevilreaper.vulpes.generator.GeneratorApplication"
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations(
            "net.onelitefeather.vulpes.*",
            "net.theevilreaper.vulpes.*"
        )
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}
