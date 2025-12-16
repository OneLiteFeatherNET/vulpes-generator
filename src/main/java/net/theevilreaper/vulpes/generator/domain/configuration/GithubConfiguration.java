package net.theevilreaper.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("github")
public record GithubConfiguration(
        String owner,
        String repo
) {
}
