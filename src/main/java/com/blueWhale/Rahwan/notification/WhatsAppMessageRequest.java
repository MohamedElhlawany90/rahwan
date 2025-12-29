package com.blueWhale.Rahwan.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppMessageRequest {

    @JsonProperty("chat_id")
    private String chatId;

    @JsonProperty("text")
    private String text;

    @JsonProperty("priority")
    private Integer priority;
}
