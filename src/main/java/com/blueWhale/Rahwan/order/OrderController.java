// ============================================
// OrderController.java (COMPLETE)
// ============================================
package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    @PostMapping(value = "/create/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationDto> createOrder(
            @PathVariable UUID userId,
            @ModelAttribute OrderForm orderForm

    ) {
        try {
            CreationDto creationDto = orderService.createOrder(orderForm, userId);
            return ResponseEntity.ok(creationDto);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

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

    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {

        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDto> getOrderStatistics(UUID userId){

        return ResponseEntity.ok(orderService.getOrderStatistics(userId));
    }
}