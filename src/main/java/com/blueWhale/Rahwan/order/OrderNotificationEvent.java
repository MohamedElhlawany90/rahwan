package com.blueWhale.Rahwan.order;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Carries all data needed for any order notification.
 * Published by OrderService → consumed by OrderNotificationListener.
 */
@Getter
public class OrderNotificationEvent extends ApplicationEvent {

    public enum Type {
        ORDER_CONFIRMED,
        DRIVER_ACCEPTED,
        DELIVERY_OTP_SENT,
        DELIVERY_CONFIRMED,
        RETURN_INITIATED,
        RETURN_CONFIRMED,
        DRIVER_CANCELLED,
        USER_CANCELLED_NO_DRIVER,
        USER_CANCELLED_WITH_DRIVER
    }

    private final Order  order;
    private final Type   type;
    private final UUID   actorId;  // driver ID when relevant
    private final String payload;  // OTP or cancellation reason

    public OrderNotificationEvent(Object source, Order order, Type type, UUID actorId, String payload) {
        super(source);
        this.order   = order;
        this.type    = type;
        this.actorId = actorId;
        this.payload = payload;
    }
}