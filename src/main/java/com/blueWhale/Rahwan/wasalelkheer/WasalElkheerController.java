package com.blueWhale.Rahwan.wasalelkheer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wasal-elkheer")
@RequiredArgsConstructor
public class WasalElkheerController {

    private final WasalElkheerService wasalElkheerService;

    /**
     * Create Wasal El-Kheer Order
     */
    @PostMapping(
            value = "/create/{userId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreationWasalElkheerDto> createOrder(
            @PathVariable UUID userId,
            @Valid @ModelAttribute WasalElkheerForm form
    ) throws IOException {

        CreationWasalElkheerDto created =
                wasalElkheerService.createWasalElkheer(form, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    /**
     * Confirm Order
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<WasalElkheerDto> confirmOrder(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.confirmOrder(id)
        );
    }

    /**
     * Get User Orders
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WasalElkheerDto>> getUserOrders(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.getUserOrders(userId)
        );
    }

    /**
     * Get Charity Orders
     */
    @GetMapping("/charity/{charityId}")
    public ResponseEntity<List<WasalElkheerDto>> getCharityOrders(
            @PathVariable Long charityId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.getCharityOrders(charityId)
        );
    }

    /**
     * Get Orders By Status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<WasalElkheerDto>> getOrdersByStatus(
            @PathVariable WasalElkheerStatus status
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.getOrdersByStatus(status)
        );
    }

    /**
     * Get Order By id
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<WasalElkheerDto> getOrderById(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.getOrderById(orderId)
        );
    }

    /**
     * Update Order Status
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<WasalElkheerDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam WasalElkheerStatus status
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.updateOrderStatus(orderId, status)
        );
    }

    /**
     * Get All Orders (Admin)
     */
    @GetMapping
    public ResponseEntity<List<WasalElkheerDto>> getAllOrders() {
        return ResponseEntity.ok(
                wasalElkheerService.getAllOrders()
        );
    }
    /**
     * Driver confirms order (accepts it)
     */
    @PostMapping("/{orderId}/driver-confirm/{driverId}")
    public ResponseEntity<WasalElkheerDto> driverConfirmOrder(
            @PathVariable Long orderId,
            @PathVariable UUID driverId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.driverConfirmOrder(orderId, driverId)
        );
    }

    /**
     * Confirm Pickup with OTP
     */
    @PostMapping("/{orderId}/confirm-pickup")
    public ResponseEntity<WasalElkheerDto> confirmPickup(
            @PathVariable Long orderId,
            @RequestParam String otp
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.confirmPickup(orderId, otp)
        );
    }

    /**
     * Update Order to IN_THE_WAY
     */
    @PostMapping("/{orderId}/in-the-way")
    public ResponseEntity<WasalElkheerDto> updateToInTheWay(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.updateToInTheWay(orderId)
        );
    }

    /**
     * Confirm Delivery with OTP
     */
    @PostMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<WasalElkheerDto> confirmDelivery(
            @PathVariable Long orderId,
            @RequestParam String otp
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.confirmDelivery(orderId, otp)
        );
    }

    /**
     * Return Order
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<WasalElkheerDto> returnOrder(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.returnOrder(orderId)
        );
    }

}
