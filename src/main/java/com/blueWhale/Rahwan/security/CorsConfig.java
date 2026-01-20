package com.blueWhale.Rahwan.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // الفرونت اللي مسموح له يدخل
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:3001"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE","PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        // مهم جدًا لو بتبعت Authorization Header أو Cookies
        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
