package com.blueWhale.Rahwan.transaction;

public enum TransactionType {

    /**
     * confirmDelivery:
     * Driver pays the sender (insuranceValue − appCommission).
     * App implicitly keeps appCommission.
     */
    DELIVERY_COMPLETED,

    /**
     * confirmReturn:
     * User pays the driver (deliveryCost × 2) as a return penalty.
     */
    RETURN_PENALTY,

    /**
     * cancelOrderByUser — only when a driver had already accepted:
     * User pays the driver (deliveryCost) as cancellation compensation.
     */
    CANCELLATION_COMPENSATION
}