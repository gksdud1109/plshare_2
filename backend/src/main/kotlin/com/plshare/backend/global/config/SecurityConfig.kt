package com.plshare.backend.global.config

import com.plshare.backend.global.security.ApplicationSessionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
class SecurityConfig(
    private val applicationSessionFilter: ApplicationSessionFilter,
) {

    @Bean
    @Profile("demo")
    fun demoSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { /* use WebConfig CORS */ }
            .headers { headers ->
                headers.frameOptions { it.disable() } // for H2 console
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(AntPathRequestMatcher("/**")).permitAll()
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
        return http.build()
    }

    @Bean
    @Profile("!demo")
    fun productionSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/auth/google/start",
                        "/api/auth/google/callback",
                        "/api/auth/spotify/start",
                        "/api/auth/spotify/callback",
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/share/**", "/api/gifts/*", "/api/tracks/*/youtube").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/gifts/*/open").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/posts/**",
                        "/api/feed",
                        "/api/users/*",
                        "/api/users/*/posts",
                        "/api/users/*/follow-stats",
                        "/api/ranking/**",
                    ).permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                applicationSessionFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java,
            )
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
        return http.build()
    }
}
