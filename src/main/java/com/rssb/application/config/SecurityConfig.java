package com.rssb.application.config;

import com.rssb.application.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the application.
 * JWT-based authentication with role-based authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/bootstrap/**").permitAll() // Bootstrap endpoints
                        .requestMatchers("/api/sewadars", "/api/sewadars/**").permitAll() // For now, allow all sewadar operations
                        .requestMatchers("/api/sql-query/**").permitAll() // SQL query endpoint for testing (NO AUTH - development only)
                        // Protected endpoints - require authentication
                        .requestMatchers("/api/programs/**").authenticated()
                        .requestMatchers("/api/program-applications/**").authenticated()
                        .requestMatchers("/api/program-selections/**").authenticated()
                        .requestMatchers("/api/actions/**").authenticated()
                        .requestMatchers("/api/action-responses/**").authenticated()
                        .requestMatchers("/api/attendances/**").authenticated()
                        // Static resources - allow all HTML, JS, CSS, and other static files
                        .requestMatchers("/", "/*.html", "/*.js", "/*.css", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.ico", "/assets/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

