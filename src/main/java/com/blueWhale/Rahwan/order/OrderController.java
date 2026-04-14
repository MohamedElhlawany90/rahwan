package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.otp.OtpRequest;
import com.blueWhale.Rahwan.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 1. User: إنشاء طلب */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationDto> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute OrderForm orderForm
    ) throws IOException {
        return ResponseEntity.ok(orderService.createOrder(orderForm, principal.getId()));
    }

    /** 2. User: تأكيد الطلب - أضفنا principal */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal  // ✅ أُضيف
    ) {
        return ResponseEntity.ok(orderService.confirmOrder(orderId, principal.getId()));
    }

    /** 3. User/Admin: تحديث الطلب */
    @PutMapping(value = "/update/{orderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationDto> updateOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute OrderForm orderForm
    ) throws IOException {
        return ResponseEntity.ok(orderService.updateOrder(orderId, orderForm, principal.getId()));
    }

    /** 4. Driver: قبول الطلب */
    @PostMapping("/{orderId}/confirm-by-driver")
    public ResponseEntity<OrderDto> driverConfirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.driverConfirmOrder(orderId, principal.getId()));
    }

    /** 5. Driver: تأكيد الاستلام */
    @PostMapping("/{orderId}/confirm-pickup")
    public ResponseEntity<OrderDto> confirmPickup(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(orderService.confirmPickup(orderId, principal.getId(), otpRequest.getOtp()));
    }

    /** 6. Driver: تأكيد التسليم */
    @PostMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<OrderDto> confirmDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(orderService.confirmDelivery(orderId, principal.getId(), otpRequest.getOtp()));
    }

    /** 7. Driver: إرجاع الطلب */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<OrderDto> returnOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.returnOrder(orderId, principal.getId()));
    }

    /** 8. Driver: في الطريق */
    @PatchMapping("/{orderId}/in-the-way")
    public ResponseEntity<OrderDto> updateToInTheWay(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.updateToInTheWay(orderId, principal.getId()));
    }

    /** 9. Driver: إلغاء الطلب */
    @PostMapping("/{orderId}/cancel-by-driver")
    public ResponseEntity<OrderDto> cancelOrderByDriver(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.cancelOrderByDriver(orderId, principal.getId()));
    }

    /** 10. User/Admin: إلغاء الطلب */
    @PostMapping("/{orderId}/cancel-by-user")
    public ResponseEntity<OrderDto> cancelOrderByUser(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(orderService.cancelOrderByUser(orderId, principal.getId(), reason));
    }

    /** 11. User: طلبات المستخدم */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getUserOrders(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    /** 12. Driver: طلبات السائق */
    @GetMapping("/driver")
    public ResponseEntity<List<OrderDto>> getDriverOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getDriverOrders(principal.getId()));
    }

    /** 13. Driver: الطلبات المتاحة - أضفنا principal */
    @GetMapping("/available")
    public ResponseEntity<List<OrderDto>> getAvailableOrders(
            @AuthenticationPrincipal UserPrincipal principal  // ✅ أُضيف
    ) {
        return ResponseEntity.ok(orderService.getAvailableOrders(principal.getId()));
    }

    /** 14. User: طلبات حسب الحالة */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getOrdersByUserAndStatus(principal.getId(), status));
    }

    /** 15. Public: تفاصيل طلب */
    @GetMapping("/{orderId}")
    public ResponseEntity<DriverDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderByIdAsDriverDto(orderId));
    }

    /** 16. Public: تتبع الطلب */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<OrderDto> trackOrder(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(orderService.getOrderByTrackingNumber(trackingNumber));
    }

    /** 17. User: عدد الطلبات */
    @GetMapping("/countByStatus")
    public ResponseEntity<OrderStatusCounts> getUserOrderCounts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getOrdersCountsByUser(principal.getId()));
    }

    /** 18. Admin: كل الطلبات - أضفنا principal */
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders(
            @AuthenticationPrincipal UserPrincipal principal  // ✅ أُضيف
    ) {
        return ResponseEntity.ok(orderService.getAllOrders(principal.getId()));
    }

    /** 19. User: إحصائيات */
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDto> getOrderStatistics(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getOrderStatistics(principal.getId()));
    }

    /** 20. Admin: تغيير حالة الطلب - أضفنا principal */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> changeOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal  // ✅ أُضيف
    ) {
        return ResponseEntity.ok(orderService.changeOrderStatus(orderId, status, principal.getId()));
    }
}