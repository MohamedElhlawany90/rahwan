// ============================================
// OrderController.java (COMPLETE)
// ============================================
package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.exception.BusinessException;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.otp.OtpRequest;
import com.blueWhale.Rahwan.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
     */
    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreationDto> createOrder(
            @AuthenticationPrincipal UserPrincipal principal, // 🔐 JWT
            @ModelAttribute OrderForm orderForm
    ) throws IOException {

        CreationDto creationDto = orderService.createOrder(orderForm, principal.getId());
        return ResponseEntity.ok(creationDto);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    /**
     * 2. Driver: قبول الطلب
     */
    @PostMapping("/{orderId}/confirm-by-driver")
    public ResponseEntity<OrderDto> driverConfirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                orderService.driverConfirmOrder(orderId, principal.getId())
        );
    }

    @PutMapping(
            value = "/update/{orderId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreationDto> updateOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal, // 🔐
            @ModelAttribute OrderForm orderForm
    ) throws IOException {

        CreationDto dto = orderService.updateOrder(orderId, orderForm, principal.getId());
        return ResponseEntity.ok(dto);
    }

    /**
     * 3. Driver: تأكيد الاستلام
     */
    @PostMapping("/{orderId}/confirm-pickup")
    public ResponseEntity<OrderDto> confirmPickup(
            @PathVariable Long orderId,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(
                orderService.confirmPickup(orderId, otpRequest.getOtp())
        );
    }

    /**
     * 4. Driver: تأكيد التسليم
     */
    @PostMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<OrderDto> confirmDelivery(
            @PathVariable Long orderId,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(
                orderService.confirmDelivery(orderId, otpRequest.getOtp())
        );
    }

    /**
     * 5. Driver: إرجاع الطلب
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<OrderDto> returnOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.returnOrder(orderId));
    }

    /**
     * 6. Driver: تحديث "في الطريق"
     */
    @PatchMapping("/{orderId}/in-the-way")
    public ResponseEntity<OrderDto> updateToInTheWay(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.updateToInTheWay(orderId));
    }

    /**
     * 7. جلب طلبات المستخدم
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getUserOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                orderService.getUserOrders(userId)
        );
    }

    /**
     * 8. جلب طلبات السائق
     */
    @GetMapping("/driver")
    public ResponseEntity<List<OrderDto>> getDriverOrders(
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                orderService.getDriverOrders(principal.getId())
        );
    }

    /**
     * 9. جلب الطلبات المتاحة
     */
    @GetMapping("/available")
    public ResponseEntity<List<OrderDto>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrders());
    }

    /**
     * 10. جلب طلبات حسب الحالة
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                orderService.getOrdersByUserAndStatus(
                        principal.getId(),
                        status
                )
        );
    }

    /**
     * 11. جلب تفاصيل طلب
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * 12. تتبع طلب (عام – بدون توكين)
     */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<OrderDto> trackOrder(
            @PathVariable String trackingNumber
    ) {
        return ResponseEntity.ok(
                orderService.getOrderByTrackingNumber(trackingNumber)
        );
    }

    @GetMapping("/countByStatus")
    public ResponseEntity<OrderStatusCounts> getUserOrderCounts(
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                orderService.getOrdersCountsByUser(principal.getId())
        );
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDto> getOrderStatistics(
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                orderService.getOrderStatistics(principal.getId())
        );
    }

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
