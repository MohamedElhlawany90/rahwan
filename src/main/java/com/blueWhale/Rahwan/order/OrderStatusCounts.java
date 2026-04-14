package com.blueWhale.Rahwan.order;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusCounts {

    private  long allOrders;
    private  long allActiveOrders;

}