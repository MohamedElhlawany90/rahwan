package com.blueWhale.Rahwan.wallet;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletDto {

    private String userId;
    private String userName;
    private double walletBalance;
    private double frozenBalance;
    private double totalBalance;
    private LocalDateTime createdAt;
}