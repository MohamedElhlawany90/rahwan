// ============================================
// OrderController.java (COMPLETE)
// ============================================
package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.order.service.OrderService;
import com.blueWhale.Rahwan.otp.OtpRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 1. User: إنشاء طلب جديد
     */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrderDto> createOrder(
            @RequestParam UUID userId,
            @RequestParam double pickupLatitude,
            @RequestParam double pickupLongitude,
            @RequestParam String pickupAddress,
            @RequestParam double recipientLatitude,
            @RequestParam double recipientLongitude,
            @RequestParam String recipientAddress,
            @RequestParam String recipientName,
            @RequestParam String recipientPhone,
            @RequestParam OrderType orderType,
            @RequestParam(required = false) double insuranceValue,
            @RequestParam(required = false) String additionalNotes,
            @Parameter(description = "Collection date (yyyy-MM-dd)", example = "2025-12-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate collectionDate,
            @Parameter(description = "Collection time (HH:mm)", example = "01:11")
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime collectionTime,
            @RequestParam Boolean anyTime,
            @RequestParam Boolean allowInspection,
            @RequestParam Boolean receiverPaysShipping,
            @Parameter(description = "Order photo", schema = @Schema(type = "string", format = "binary"))
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        OrderForm form = new OrderForm();
        form.setPickupLatitude(pickupLatitude);
        form.setPickupLongitude(pickupLongitude);
        form.setPickupAddress(pickupAddress);
        form.setRecipientLatitude(recipientLatitude);
        form.setRecipientLongitude(recipientLongitude);
        form.setRecipientAddress(recipientAddress);
        form.setRecipientName(recipientName);
        form.setRecipientPhone(recipientPhone);
        form.setOrderType(orderType);
        form.setInsuranceValue(insuranceValue);
        form.setAdditionalNotes(additionalNotes);
        form.setCollectionDate(collectionDate);
        form.setCollectionTime(collectionTime);
        form.setAnyTime(anyTime);
        form.setAllowInspection(allowInspection);
        form.setReceiverPaysShipping(receiverPaysShipping);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(form, userId, photo));
    }
//
//    /**
//     * 2. Driver: قبول الطلب
//     */
//    @PostMapping("/{orderId}/confirm-by-driver")
//    public ResponseEntity<OrderDto> driverConfirmOrder(
//            @PathVariable Long orderId,
//            @RequestParam UUID driverId) {
//        return ResponseEntity.ok(orderService.driverConfirmOrder(orderId, driverId));
//    }
//
//    /**
//     * 3. Driver: تأكيد الاستلام
//     */
//    @PostMapping("/{orderId}/confirm-pickup")
//    public ResponseEntity<OrderDto> confirmPickup(
//            @PathVariable Long orderId,
//            @Valid @RequestBody OtpRequest otpRequest) {
//        return ResponseEntity.ok(orderService.confirmPickup(orderId, otpRequest.getOtp()));
//    }
//
//    /**
//     * 4. Driver: تأكيد التسليم
//     */
//    @PostMapping("/{orderId}/confirm-delivery")
//    public ResponseEntity<OrderDto> confirmDelivery(
//            @PathVariable Long orderId,
//            @Valid @RequestBody OtpRequest otpRequest) {
//        return ResponseEntity.ok(orderService.confirmDelivery(orderId, otpRequest.getOtp()));
//    }
//
//    /**
//     * 5. Driver: إرجاع الطلب
//     */
//    @PostMapping("/{orderId}/return")
//    public ResponseEntity<OrderDto> returnOrder(@PathVariable Long orderId) {
//        return ResponseEntity.ok(orderService.returnOrder(orderId));
//    }
//
//    /**
//     * 6. Driver: تحديث "في الطريق"
//     */
//    @PatchMapping("/{orderId}/in-the-way")
//    public ResponseEntity<OrderDto> updateToInTheWay(@PathVariable Long orderId) {
//        return ResponseEntity.ok(orderService.updateToInTheWay(orderId));
//    }

    /**
     * 7. جلب طلبات المستخدم
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getUserOrders(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

//    /**
//     * 8. جلب طلبات السائق
//     */
//    @GetMapping("/driver/{driverId}")
//    public ResponseEntity<List<OrderDto>> getDriverOrders(@PathVariable UUID driverId) {
//        return ResponseEntity.ok(orderService.getDriverOrders(driverId));
//    }
//
//    /**
//     * 9. جلب الطلبات المتاحة
//     */
//    @GetMapping("/available")
//    public ResponseEntity<List<OrderDto>> getAvailableOrders() {
//        return ResponseEntity.ok(orderService.getAvailableOrders());
//    }

    /**
     * 10. جلب طلبات حسب الحالة
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserAndStatus(userId, status));
    }

    /**
     * 11. جلب تفاصيل طلب
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * 12. تتبع طلب
     */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<OrderDto> trackOrder(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(orderService.getOrderByTrackingNumber(trackingNumber));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    @GetMapping("/user/{userId}/countByStatus")
    public ResponseEntity<OrderStatusCounts> getUserOrderCounts(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderService.getOrdersCountsByUser(userId));
    }


}