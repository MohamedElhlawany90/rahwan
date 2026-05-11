package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.notification.WhatsAppService;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final WhatsAppService whatsAppService;
    private final UserRepository  userRepository;

    @Async
    @TransactionalEventListener
    public void handle(OrderNotificationEvent event) {
        Order order = event.getOrder();

        switch (event.getType()) {

            case ORDER_CONFIRMED -> {
                User sender = findUser(order.getUserId());
                whatsAppService.sendPickupOtpToSender(
                        sender.getPhone(), sender.getName(), event.getPayload());
                whatsAppService.sendOrderConfirmation(
                        sender.getPhone(), order.getTrackingNumber(), order.getDeliveryCost());
            }

            case DRIVER_ACCEPTED -> {
                User sender = findUser(order.getUserId());
                User driver = findUser(event.getActorId());
                whatsAppService.sendDriverAcceptedNotification(
                        sender.getPhone(), driver.getName(), order.getTrackingNumber());
            }

            case DELIVERY_OTP_SENT -> {
                whatsAppService.sendDeliveryOtpToRecipient(
                        order.getRecipientPhone(), order.getRecipientName(), event.getPayload());
            }

            case DELIVERY_CONFIRMED -> {
                User sender = findUser(order.getUserId());
                whatsAppService.sendDeliveryConfirmation(sender.getPhone(), order.getTrackingNumber());
            }

            case RETURN_INITIATED -> {
                User sender = findUser(order.getUserId());
                whatsAppService.sendReturnOrderOtp(sender.getPhone(), event.getPayload());
            }

            case RETURN_CONFIRMED -> {
                User sender = findUser(order.getUserId());
                whatsAppService.sendOrderCancellation(sender.getPhone(), order.getTrackingNumber(),
                        "تم إرجاع الطلب وتحويل تكلفة الشحن المضاعفة للسائق");
            }

            case DRIVER_CANCELLED -> {
                User sender = findUser(order.getUserId());
                whatsAppService.send(sender.getPhone(),
                        "⚠️ السائق ألغى طلبك\n\n📦 رقم التتبع: " + order.getTrackingNumber()
                                + "\n\n🔄 طلبك الآن متاح للسواقين مرة أخرى، سيتم إبلاغك حين يقبله سائق جديد."
                                + "\n\n---\n⚠️ Your driver cancelled your order\n📦 Tracking: " + order.getTrackingNumber()
                                + "\n🔄 Your order is back in the pool. We'll notify you when a new driver picks it up.");
            }

            case USER_CANCELLED_NO_DRIVER -> {
                User sender = findUser(order.getUserId());
                String reason = event.getPayload() != null ? event.getPayload() : "Cancelled by user";
                whatsAppService.sendOrderCancellation(
                        sender.getPhone(), order.getTrackingNumber(), reason);
            }

            case USER_CANCELLED_WITH_DRIVER -> {
                User driver = findUser(order.getDriverId());
                whatsAppService.sendOrderCancellation(
                        driver.getPhone(), order.getTrackingNumber(),
                        "User cancelled the order. You received "
                                + order.getDeliveryCost() + " EGP as compensation.");
            }
        }
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}