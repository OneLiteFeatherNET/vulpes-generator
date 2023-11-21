package net.theevilreaper.vulpes.generator.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("gitlab")
data class GitlabProperties(
    val accessToken: String,
    val gitlabUrl: String,
    val baseProjectID: Int

)
