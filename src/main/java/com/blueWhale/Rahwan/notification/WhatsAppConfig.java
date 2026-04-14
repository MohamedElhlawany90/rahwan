package com.blueWhale.Rahwan.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "whatsapp.api")
@Getter
@Setter
public class WhatsAppConfig {
    private String baseUrl;
    private String token;
    private String instanceId;
}