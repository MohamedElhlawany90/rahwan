package com.blueWhale.Rahwan.wallet;

import lombok.Data;


@Data
public class WalletDto {

    private String userId;
    private double balance;
    private double frozenBalance;
}
