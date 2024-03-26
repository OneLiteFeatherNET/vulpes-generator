package net.theevilreaper.vulpes.generator.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository

@Configuration
@EnableWebSecurity
class SpringSecurityConfiguration {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it.requestMatchers("/**").permitAll()
        }
            .csrf { it.csrfTokenRepository(csrfTokenRepository()) }

        return http.build()
    }

    @Bean
    fun csrfTokenRepository(): CsrfTokenRepository {
        val csrfTokenRepository = HttpSessionCsrfTokenRepository()
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN")
        return csrfTokenRepository
    }
}
