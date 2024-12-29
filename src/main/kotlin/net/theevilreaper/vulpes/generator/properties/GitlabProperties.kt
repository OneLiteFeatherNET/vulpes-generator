package net.theevilreaper.vulpes.generator.properties

import io.micronaut.context.annotation.ConfigurationProperties
import jakarta.inject.Singleton

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
@Singleton
@ConfigurationProperties(value = "gitlab")
data class GitlabProperties(
    var user: String,
    var password: String,
    var accessToken: String,
    var gitlabUrl: String,
    var baseProjectID: Int,
    var remoteUrl: String,
    var piplineUrl: String,
    var deployUrl: String,
    var dependencyUrl: String,
)
