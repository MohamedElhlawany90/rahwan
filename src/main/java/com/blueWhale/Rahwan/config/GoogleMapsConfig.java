package com.blueWhale.Rahwan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.maps.api")
@Data
public class GoogleMapsConfig {
    private String key;
    private String baseUrl;
    private int timeout = 5000; // milliseconds
    private int maxRetries = 2;
}