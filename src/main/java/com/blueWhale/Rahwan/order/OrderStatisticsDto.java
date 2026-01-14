package com.blueWhale.Rahwan.order;

import lombok.Data;

@Data
public class OrderStatisticsDto {

    private long totalOrders;
    private long pending;
    private long accepted;
    private long inProgress;
    private long inTheWay;
    private long inDelivery;
    private long delivered;
    private long cancelled;
}
