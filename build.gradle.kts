plugins {
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
    annotationProcessor(mn.micronaut.openapi)

    compileOnly(mn.micronaut.openapi.annotations)

    annotationProcessor("io.micronaut:micronaut-inject-java:4.8.13")
    implementation(mn.micronaut.data.processor)


    implementation(libs.bundles.vulpes)
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
        options.release = 21
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
