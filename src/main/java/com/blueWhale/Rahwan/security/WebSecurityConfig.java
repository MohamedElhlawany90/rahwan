package com.blueWhale.Rahwan.security;

import org.springframework.context.annotation.Bean;


import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/api/**",
                                "/api/test/all",
                                "/uploads/**",
                                "/uploads/images/**",
                                "/uploads/documents/**",
                                "/uploads/videos/**",
                                "/uploads/audio/**",
                                "/uploads/others/**",
                                "/uploads/icons/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }

}
