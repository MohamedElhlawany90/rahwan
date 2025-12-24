package com.blueWhale.Rahwan.otp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "twilio")
@Getter
@Setter
public class TwilioProperties {
    private String accountSid;
    private String authToken;
    private String phoneNumber;
}
