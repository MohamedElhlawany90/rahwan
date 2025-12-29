package com.blueWhale.Rahwan.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class WhatsAppClient {

    private final WhatsAppConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    public WhatsAppMessageResponse sendMessage(WhatsAppMessageRequest request) {

        String url = config.getBaseUrl() + "/" + config.getInstanceId() + "/send-message";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", config.getToken());

        HttpEntity<WhatsAppMessageRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<WhatsAppMessageResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    WhatsAppMessageResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            System.err.println("Failed to send WhatsApp message: " + e.getMessage());
            throw new RuntimeException("WhatsApp API error: " + e.getMessage());
        }
    }
}