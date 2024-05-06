package net.theevilreaper.vulpes.generator

import net.theevilreaper.vulpes.api.RepoSpec
import net.theevilreaper.vulpes.generator.properties.GitlabProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@EnableMongoRepositories(
    basePackageClasses = [
        RepoSpec::class,
    ]
)
@SpringBootApplication(
    scanBasePackageClasses = [
        GeneratorApplication::class,
        RepoSpec::class
    ]
)
@EnableConfigurationProperties(value = [GitlabProperties::class])
class GeneratorApplication()


fun main(args: Array<String>) {
    // https://stackoverflow.com/a/48988779
    runApplication<GeneratorApplication>(*args)
}


