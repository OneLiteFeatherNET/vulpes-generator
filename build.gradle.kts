import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.*

plugins {
	alias(libs.plugins.spring)
	alias(libs.plugins.spring.dependency)
	alias(libs.plugins.kotlin)
	alias(libs.plugins.kotlin.spring)
	jacoco
}

group = "net.theevilreaper"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

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
		url = if (System.getenv().containsKey("CI")) {
			val ciApiv4Url = System.getenv("CI_API_V4_URL")
			uri("$ciApiv4Url/groups/$groupdId/-/packages/maven")
		} else {
			uri("https://gitlab.themeinerlp.dev/api/v4/groups/$groupdId/-/packages/maven")
		}
		name = "GitLab"
		credentials(HttpHeaderCredentials::class.java) {
			name = if (System.getenv().containsKey("CI")) {
				"Job-Token"
			} else {
				"Private-Token"
			}
			value = if (System.getenv().containsKey("CI")) {
				System.getenv("CI_JOB_TOKEN")
			} else {
				val gitLabPrivateToken: String? by project
				gitLabPrivateToken
			}
		}
		authentication {
			create<HttpHeaderAuthentication>("header")
		}
	}
}

dependencies {
	implementation("net.theevilreaper.vulpes.api:vulpes-spring-api:0.0.1-SNAPSHOT")
	implementation("dev.themeinerlp:dartpoet:0.0.1-SNAPSHOT")
	implementation("org.springframework.boot:spring-boot-starter-log4j2")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.6")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains:annotations:24.0.1")
	implementation("com.squareup:javapoet:1.13.0")
	implementation("dev.themeinerlp:vulpes-base:1.0-SNAPSHOT+95bd27ce") {
		exclude(group = "com.github.Minestom", module = "Minestom")
		exclude(group = "net.worldseed.multipart", module = "WorldSeedEntityEngine")
	}

	implementation("net.onelitefeather.microtus:Minestom:1.1.0") {
		exclude(group = "org.tinylog", module = "slf4j-tinylog")
	}

	implementation("org.eclipse.jgit:org.eclipse.jgit:6.5.0.202303070854-r")
	implementation("org.gitlab4j:gitlab4j-api:5.0.1")
	implementation("com.google.guava:guava:31.1-jre")
	implementation("commons-io:commons-io:2.11.0")
	implementation("org.apache.commons:commons-compress:1.21")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.+")
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
			jvmTarget = JvmTarget.JVM_17
		}
	}

	bootBuildImage {
		if (System.getenv().containsKey("CI")) {
			createdDate.set(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(Date()))
			environment.set(environment.get() + mapOf("BP_JVM_VERSION" to "17"))
			imageName.set("${System.getenv("CI_REGISTRY_IMAGE")}/generator")
			publish.set(true)
			docker {
				publishRegistry {
					url.set("https://${System.getenv("CI_REGISTRY")}")
					username.set(System.getenv("CI_REGISTRY_USER"))
					password.set(System.getenv("CI_REGISTRY_PASSWORD"))
				}
			}
		}
	}
	bootJar {
		duplicatesStrategy = DuplicatesStrategy.INCLUDE
	}
}

springBoot {
	buildInfo()
}

configurations {
	all {
		exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
	}
}
