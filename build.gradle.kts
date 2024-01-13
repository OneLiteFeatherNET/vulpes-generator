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
	targetCompatibility = JavaVersion.VERSION_17
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
	implementation(libs.bundles.vulpes) {
		exclude(group = "com.github.Minestom", module = "Minestom")
		exclude(group = "net.worldseed.multipart", module = "WorldSeedEntityEngine")
	}
	implementation(libs.dartpoet)
	implementation(libs.bundles.spring)
	implementation(libs.caffeine)
	implementation(libs.jetbrains.annotation)
	implementation(libs.javapoet)
	implementation(libs.microtus) {
		exclude(group = "org.tinylog", module = "slf4j-tinylog")
	}

	implementation(libs.jgit)
	implementation(libs.gitlab4j)
	implementation(libs.guava)
	implementation(libs.commons.io)
	implementation(libs.commons.compress)
	implementation(libs.jackson)
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
