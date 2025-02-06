import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.micronaut.aot)
    jacoco
}

group = "net.theevilreaper"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    annotationProcessor(mn.micronaut.serde.processor)
    annotationProcessor(mn.micronaut.http.validation)
    annotationProcessor(mn.micronaut.data.processor)
    annotationProcessor("io.micronaut:micronaut-inject-java:4.7.10")
    annotationProcessor(mn.micronaut.openapi)
    implementation(mn.micronaut.data.processor)
    compileOnly(mn.micronaut.openapi.annotations)

    implementation(libs.bundles.vulpes) {
        exclude(group = "com.github.Minestom", module = "Minestom")
        exclude(group = "net.worldseed.multipart", module = "WorldSeedEntityEngine")
    }
    implementation(libs.jetbrains.annotation)
    implementation(libs.javapoet)
    implementation(libs.microtus) {
        exclude(group = "org.tinylog", module = "slf4j-tinylog")
    }

    //Micronaut
    implementation(mn.micronaut.runtime)
    implementation(mn.validation)
    implementation(mn.snakeyaml)
    implementation(mn.log4j)
    implementation(mn.slf4j.api)
    implementation(mn.slf4j.simple)
    implementation(mn.jackson.core)
    implementation(mn.jackson.databind)
    implementation(mn.jackson.datatype.jsr310)

    implementation(mn.micronaut.data.document.processor)
    implementation(mn.micronaut.data.mongodb)
    implementation(mn.micronaut.mongo.core)

    implementation(libs.jgit)
    implementation(libs.gitlab4j)
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(libs.commons.compress)

    testImplementation(mn.junit.jupiter.api)
    testRuntimeOnly(mn.junit.jupiter.engine)
}

tasks {
    create("gradle template zip", Zip::class) {
        exclude("DS_Store")
        archiveFileName.set("gradle_template.zip")
        destinationDirectory.set(file("src/main/resources"))
        from("${rootDir}/assets/gradle_template")
    }

    create("gradle server template zip", Zip::class) {
        exclude("DS_STORE")
        archiveFileName.set("test-server.zip")
        destinationDirectory.set(file("src/main/resources"))
        from("${rootDir}/assets/test_server")
    }

    create("gradle gitlab zip", Zip::class) {
        exclude("DS_STORE")
        archiveFileName.set("gitlab-ci.zip")
        destinationDirectory.set(file("src/main/resources"))
        from("${rootDir}/assets/gitlab")
    }

    create("copyGitlabCiFile", Copy::class) {
        from("$rootDir/assets/gitlab/.gitlab-ci.yml")
        into("src/main/resources")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    processResources {
        dependsOn("gradle template zip")
        dependsOn("gradle server template zip")
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

    compileKotlin {
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JvmTarget.JVM_21
        }
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
            "net.theevilreaper.vulpes.api.*",
            "net.theevilreaper.vulpes.generator.*",
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
