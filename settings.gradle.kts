rootProject.name = "vulpes-spring-generator"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.21")

            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin.spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")
            plugin("spring", "org.springframework.boot").version("3.1.5")
            plugin("spring.dependency", "io.spring.dependency-management").version("1.1.4")
        }
    }
}
