package com.openinsure.claim.security;

import java.io.IOException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain claimSecurity(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/internal/**").authenticated()
                        .requestMatchers("/consents/*/revoke", "/consents").hasAuthority("SCOPE_consent:write")
                        .requestMatchers("/consents/**").hasAuthority("SCOPE_consent:read")
                        .requestMatchers("/policies/**").hasAuthority("SCOPE_policy:read")
                        .requestMatchers("/claims").hasAuthority("SCOPE_claim:write")
                        .requestMatchers("/claims/**").hasAuthority("SCOPE_claim:read")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(problemAuthenticationEntryPoint())
                        .accessDeniedHandler(problemAccessDeniedHandler())
                        .jwt(jwt -> {
                        }));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
    SecurityFilterChain disabledSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }

    private AuthenticationEntryPoint problemAuthenticationEntryPoint() {
        return (request, response, exception) -> writeProblem(response, HttpStatus.UNAUTHORIZED,
                "Authentication required");
    }

    private AccessDeniedHandler problemAccessDeniedHandler() {
        return (request, response, exception) -> writeProblem(response, HttpStatus.FORBIDDEN, "Insufficient scope");
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = String.format(
                """
                        {"type":"https://openinsure.local/problems/%d","title":"%s","status":%d,"detail":"%s","correlationId":"%s"}
                        """,
                status.value(), status.getReasonPhrase(), status.value(), detail, UUID.randomUUID());
        response.getWriter().write(body);
    }
}
