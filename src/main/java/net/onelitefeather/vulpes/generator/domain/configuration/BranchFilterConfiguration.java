package net.onelitefeather.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.List;

/**
 * The {@link BranchFilterConfiguration} contains the configuration which branches should be excluded when fetching them from Github
 * @param exclude a list of branch name prefixes which should be excluded
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties("filter")
public record BranchFilterConfiguration(
        List<String> exclude
) {
}
