package com.blueWhale.Rahwan.transaction;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransactionDto {

    private Long id;
    private Long orderId;
    private String trackingNumber;

    private UUID fromUserId;
    private String fromUserName;   // enriched from UserRepository

    private UUID toUserId;
    private String toUserName;     // enriched from UserRepository

    private double amount;
    private double appCommission;

    private TransactionType type;
    private String description;
    private LocalDateTime createdAt;
}