package com.blueWhale.Rahwan.order;

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

/**
 * Unified controller for all order types.
 *
 * REGULAR orders  → /api/orders/...
 * CHARITY orders  → /api/orders/charity/...
 *
 * Shared lifecycle endpoints (confirm, pickup, delivery, cancel, etc.)
 * are at /api/orders/{orderId}/... and work for both types — the service
 * branches internally based on OrderCategory.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ═══════════════════════════════════════════════════════════════════
    //  REGULAR order creation & update
    // ═══════════════════════════════════════════════════════════════════

    /** 1. User: create a regular delivery order */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationDto> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute OrderForm orderForm
    ) throws IOException {
        return ResponseEntity.ok(orderService.createOrder(orderForm, principal.getId()));
    }

    /** 3. User/Admin: update a regular delivery order */
    @PutMapping(value = "/update/{orderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationDto> updateOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute OrderForm orderForm
    ) throws IOException {
        return ResponseEntity.ok(orderService.updateOrder(orderId, orderForm, principal.getId()));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CHARITY order creation & update  (/charity sub-path)
    // ═══════════════════════════════════════════════════════════════════

    /** 1-B. User: create a charity donation order (WasalElkheer) */
    @PostMapping(value = "/charity/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationDto> createCharityOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute CharityOrderForm form
    ) throws IOException {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createCharityOrder(form, principal.getId()));
    }

    /** 3-B. User/Admin: update a charity donation order */
    @PutMapping(value = "/charity/update/{orderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationDto> updateCharityOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute CharityOrderForm form
    ) throws IOException {
        return ResponseEntity.ok(orderService.updateCharityOrder(orderId, form, principal.getId()));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Shared lifecycle  (works for both REGULAR and CHARITY)
    // ═══════════════════════════════════════════════════════════════════

    /** 2. User: confirm order (REGULAR freezes wallet; CHARITY does not) */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.confirmOrder(orderId, principal.getId()));
    }

    /** 4. Driver: accept an available order */
    @PostMapping("/{orderId}/confirm-by-driver")
    public ResponseEntity<OrderDto> driverConfirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.driverConfirmOrder(orderId, principal.getId()));
    }

    /** 5. Driver: confirm pickup with OTP */
    @PostMapping("/{orderId}/confirm-pickup")
    public ResponseEntity<OrderDto> confirmPickup(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(orderService.confirmPickup(orderId, principal.getId(), otpRequest.getOtp()));
    }

    /** 6. Driver: confirm delivery with OTP */
    @PostMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<OrderDto> confirmDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(orderService.confirmDelivery(orderId, principal.getId(), otpRequest.getOtp()));
    }

    /** 7-A. Driver: initiate return — REGULAR orders only */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<OrderDto> returnOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.returnOrder(orderId, principal.getId()));
    }

    /** 7-B. Driver: confirm return with OTP — REGULAR orders only */
    @PostMapping("/{orderId}/confirm-return")
    public ResponseEntity<OrderDto> confirmReturn(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OtpRequest otpRequest
    ) {
        return ResponseEntity.ok(orderService.confirmReturn(orderId, principal.getId(), otpRequest.getOtp()));
    }

    /** 8. Driver: cancel an accepted order (order returns to PENDING) */
    @PostMapping("/{orderId}/cancel-by-driver")
    public ResponseEntity<OrderDto> cancelOrderByDriver(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.cancelOrderByDriver(orderId, principal.getId()));
    }

    /** 9. User/Admin: cancel an order */
    @PostMapping("/{orderId}/cancel-by-user")
    public ResponseEntity<OrderDto> cancelOrderByUser(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(orderService.cancelOrderByUser(orderId, principal.getId(), reason));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Queries — general
    // ═══════════════════════════════════════════════════════════════════

    /** My orders (all types) */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getUserOrders(principal.getId()));
    }

    /** My orders as a driver */
    @GetMapping("/driver")
    public ResponseEntity<List<OrderDto>> getDriverOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getDriverOrders(principal.getId()));
    }

    /** Available orders for driver — all types */
    @GetMapping("/available")
    public ResponseEntity<List<OrderDto>> getAvailableOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getAvailableOrders(principal.getId()));
    }

    /** Available REGULAR orders only */
    @GetMapping("/available/regular")
    public ResponseEntity<List<OrderDto>> getAvailableRegularOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.getAvailableOrdersByCategory(principal.getId(), OrderCategory.REGULAR));
    }

    /** Available CHARITY orders only */
    @GetMapping("/available/charity")
    public ResponseEntity<List<OrderDto>> getAvailableCharityOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                orderService.getAvailableOrdersByCategory(principal.getId(), OrderCategory.CHARITY));
    }

    /** User: orders by status */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getOrdersByUserAndStatus(principal.getId(), status));
    }

    /** Public: order details */
    @GetMapping("/{orderId}")
    public ResponseEntity<DriverDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderByIdAsDriverDto(orderId));
    }

    /** Public: track by tracking number */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<OrderDto> trackOrder(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(orderService.getOrderByTrackingNumber(trackingNumber));
    }

    /** User: order counts */
    @GetMapping("/countByStatus")
    public ResponseEntity<OrderStatusCounts> getUserOrderCounts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getOrdersCountsByUser(principal.getId()));
    }

    /** User: statistics */
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDto> getOrderStatistics(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getOrderStatistics(principal.getId()));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Admin
    // ═══════════════════════════════════════════════════════════════════

    /** Admin: all confirmed orders */
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getAllOrders(principal.getId()));
    }

    /** User: all confirmed orders for current user */
    @GetMapping("/user")
    public ResponseEntity<List<OrderDto>> getAllOrdersForUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getAllOrdersForUser(principal.getId()));
    }

    /** Admin: force-change order status */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> changeOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.changeOrderStatus(orderId, status, principal.getId()));
    }
}