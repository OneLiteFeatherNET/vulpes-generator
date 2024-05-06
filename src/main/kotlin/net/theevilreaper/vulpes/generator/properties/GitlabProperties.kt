package net.theevilreaper.vulpes.generator.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The class contains some properties which are required to communicate with the Gitlab API.
 * It important to use that class to avoid hard coded values and prevent the exposure of sensitive data.
 * @property accessToken The access token to authenticate against the Gitlab API
 * @property gitlabUrl The URL to the Gitlab instance
 * @property baseProjectID The ID of the base project
 * @see ConfigurationProperties
 * @since 1.0.0
 * @version 1.0.0
 * @author theEvilReaper
 */
@ConfigurationProperties("gitlab")
data class GitlabProperties(
    val user: String,
    val password: String,
    val accessToken: String,
    val gitlabUrl: String,
    val baseProjectID: Int,
    val remoteUrl: String,
    val piplineUrl: String,
    val deployUrl: String,
    val dependencyUrl: String,
)
