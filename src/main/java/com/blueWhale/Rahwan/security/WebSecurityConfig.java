package com.blueWhale.Rahwan.security;

import org.springframework.context.annotation.Bean;


import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF
                .cors(AbstractHttpConfigurer::disable) // Disable CORS (or configure if needed)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).httpBasic(HttpBasicConfigurer::disable).formLogin(FormLoginConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/advertisements/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/advertisements/**",
                                "//api/charities**",
                                "/swagger-ui/index.html",
                                "/api/**",
                                "/api/address/**",
                                "/api/test/all",
                                "/uploads/**"

                        ).permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }

}
