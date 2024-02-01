rootProject.name = "vulpes"
rootProject.buildFileName = "build.gradle.kts"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("jetbrains.annotation", "org.jetbrains", "annotations").version("24.1.0")
            library("microtus", "net.onelitefeather.microtus", "Minestom").version("1.2.1")
        }
    }
}
