rootProject.name = "vulpes"
rootProject.buildFileName = "build.gradle.kts"

dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("26.0.2")
            library("microtus", "net.onelitefeather.microtus", "Minestom").version("1.5.0")
        }
    }
}
