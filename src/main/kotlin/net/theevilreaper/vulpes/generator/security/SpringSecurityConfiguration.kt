package net.theevilreaper.vulpes.generator.security

/*@Configuration
@EnableWebSecurity
class SpringSecurityConfiguration {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {

        http.authorizeHttpRequests {
            it.requestMatchers("/**").permitAll()
        }
            .cors { it.configurationSource { corsConfiguration() } }
            .csrf { it.csrfTokenRepository(csrfTokenRepository()) }

        return http.build()
    }

    @Bean
    fun csrfTokenRepository(): CsrfTokenRepository {
        val csrfTokenRepository = HttpSessionCsrfTokenRepository()
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN")
        return csrfTokenRepository
    }

    @Bean
    fun corsConfiguration(): CorsConfiguration {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        return configuration
    }
}*/