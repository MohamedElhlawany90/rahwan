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

    /**
     * 1. User: إنشاء طلب جديد
     * Authorization: Any authenticated user
     */
    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreationDto> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute OrderForm orderForm
    ) throws IOException {
        CreationDto creationDto = orderService.createOrder(orderForm, principal.getId());
        return ResponseEntity.ok(creationDto);
    }

    /**
     * 2. User: تأكيد الطلب (تجميد المبلغ)
     * Authorization: Order owner
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(
            @PathVariable Long orderId
            // ❌ لا نحتاج principal - السيرفس يتحقق من الـ order نفسه
    ) {
        return ResponseEntity.ok(orderService.confirmOrder(orderId));
    }

    /**
     * 3. User/Admin: تحديث الطلب
     * Authorization: Order owner or Admin
     */
    @PutMapping(
            value = "/update/{orderId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreationDto> updateOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute OrderForm orderForm
    ) throws IOException {
        CreationDto dto = orderService.updateOrder(orderId, orderForm, principal.getId());
        return ResponseEntity.ok(dto);
    }

    /**
     * 4. Driver: قبول الطلب
     * Authorization: Driver only
     */
    @PostMapping("/{orderId}/confirm-by-driver")
    public ResponseEntity<OrderDto> driverConfirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.driverConfirmOrder(orderId, principal.getId())
        );
    }

    /**
     * 5. Driver: تأكيد الاستلام
     * Authorization: Driver who accepted the order
     */
    @PostMapping("/{orderId}/confirm-pickup")
    public ResponseEntity<OrderDto> confirmPickup(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(
                orderService.confirmPickup(orderId, principal.getId(), otpRequest.getOtp())
        );
    }

    /**
     * 6. Driver: تأكيد التسليم
     * Authorization: Driver who accepted the order
     */
    @PostMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<OrderDto> confirmDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(
                orderService.confirmDelivery(orderId, principal.getId(), otpRequest.getOtp())
        );
    }

    /**
     * 7. Driver: إرجاع الطلب
     * Authorization: Driver who accepted the order
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<OrderDto> returnOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.returnOrder(orderId, principal.getId())
        );
    }

    /**
     * 8. Driver: تحديث "في الطريق"
     * Authorization: Driver who accepted the order
     */
    @PatchMapping("/{orderId}/in-the-way")
    public ResponseEntity<OrderDto> updateToInTheWay(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.updateToInTheWay(orderId, principal.getId())
        );
    }

    /**
     * 9. Driver: إلغاء الطلب (قبل القبول فقط)
     * Authorization: Driver
     */
    @PostMapping("/{orderId}/cancel-by-driver")
    public ResponseEntity<OrderDto> cancelOrderByDriver(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.cancelOrderByDriver(orderId, principal.getId())
        );
    }

    /**
     * 10. User/Admin: إلغاء الطلب
     * Authorization: Order owner or Admin
     */
    @PostMapping("/{orderId}/cancel-by-user")
    public ResponseEntity<OrderDto> cancelOrderByUser(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(
                orderService.cancelOrderByUser(orderId, principal.getId(), reason)
        );
    }

    /**
     * 11. User: جلب طلبات المستخدم
     * Authorization: Any authenticated user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getUserOrders(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    /**
     * 12. Driver: جلب طلبات السائق
     * Authorization: Driver
     */
    @GetMapping("/driver")
    public ResponseEntity<List<OrderDto>> getDriverOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.getDriverOrders(principal.getId())
        );
    }

    /**
     * 13. جلب الطلبات المتاحة
     * Authorization: Public (or Driver)
     */
    @GetMapping("/available")
    public ResponseEntity<List<OrderDto>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrders());
    }

    /**
     * 14. User: جلب طلبات حسب الحالة
     * Authorization: Any authenticated user
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.getOrdersByUserAndStatus(principal.getId(), status)
        );
    }

    /**
     * 15. جلب تفاصيل طلب
     * Authorization: Public (or authenticated user)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * 16. تتبع طلب (عام – بدون توكين)
     * Authorization: Public
     */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<OrderDto> trackOrder(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(
                orderService.getOrderByTrackingNumber(trackingNumber)
        );
    }

    /**
     * 17. User: عدد الطلبات حسب الحالة
     * Authorization: Any authenticated user
     */
    @GetMapping("/countByStatus")
    public ResponseEntity<OrderStatusCounts> getUserOrderCounts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.getOrdersCountsByUser(principal.getId())
        );
    }

    /**
     * 18. Admin: جلب كل الطلبات
     * Authorization: Admin only
     */
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * 19. User: إحصائيات الطلبات
     * Authorization: Any authenticated user
     */
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDto> getOrderStatistics(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.getOrderStatistics(principal.getId())
        );
    }

    /**
     * 20. Admin: تغيير حالة الطلب
     * Authorization: Admin only
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> changeOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status
    ) {
        return ResponseEntity.ok(
                orderService.changeOrderStatus(orderId, status)
        );
    }
}