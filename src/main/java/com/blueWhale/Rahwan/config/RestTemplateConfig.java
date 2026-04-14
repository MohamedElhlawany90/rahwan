package com.blueWhale.Rahwan.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * ⚙️ إعدادات RestTemplate للاتصال بـ Google Maps API
 *
 * تم تحديثه ليتوافق مع Spring Boot 3.4.0+
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, GoogleMapsConfig googleMapsConfig) {
        return builder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(googleMapsConfig.getTimeout());
                    factory.setReadTimeout(googleMapsConfig.getTimeout());
                    return factory;
                })
                .build();
    }
}