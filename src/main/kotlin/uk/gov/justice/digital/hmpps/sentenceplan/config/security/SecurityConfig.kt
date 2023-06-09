package uk.gov.justice.digital.hmpps.sentenceplan.config.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnDefaultWebSecurity
class SecurityConfig {
  fun filterChain(http: HttpSecurity): HttpSecurity {
    http.authorizeHttpRequests {
      it.requestMatchers(
        AntPathRequestMatcher("/actuator/**"),
        AntPathRequestMatcher("/health/**"),
        AntPathRequestMatcher("/info/**"),
        AntPathRequestMatcher("/swagger-ui/**"),
        AntPathRequestMatcher("/v3/api-docs.yaml"),
        AntPathRequestMatcher("/v3/api-docs/**"),
      ).permitAll().anyRequest().authenticated()
    }
      .csrf().disable()
      .cors().disable()
      .httpBasic().disable()
      .formLogin().disable()
      .logout().disable()
      .oauth2ResourceServer {
        it.jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
      }
    return http
  }

  @Bean
  @ConditionalOnMissingBean
  fun configure(http: HttpSecurity): SecurityFilterChain = filterChain(http).build()
}
