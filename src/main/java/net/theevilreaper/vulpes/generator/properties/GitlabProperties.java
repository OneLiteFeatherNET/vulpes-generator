package net.theevilreaper.vulpes.generator.properties;

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.inject.Singleton;

@Singleton
@ConfigurationProperties("gitlab")
public record GitlabProperties(
        String user,
        String password,
        String accessToken,
        String gitlabUrl,
        int baseProjectId,
        String remoteUrl,
        String pipelineUrl,
        String deployUrl,
        String dependencyUrl
) {
}
