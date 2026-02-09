package com.blueWhale.Rahwan.commission;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyRevenueDto {
    private String month;
    private double revenue;
}