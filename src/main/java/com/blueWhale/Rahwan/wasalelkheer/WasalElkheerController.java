package com.blueWhale.Rahwan.wasalelkheer;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.order.OrderDto;
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
@RequestMapping("/api/wasalElkheer")
@RequiredArgsConstructor
public class WasalElkheerController {

    private final WasalElkheerService wasalElkheerService;

    @PostMapping(value = "/create/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreationWasalElkheerDto> createOrder(
            @PathVariable UUID userId,
            @Valid @ModelAttribute WasalElkheerForm form) {
        try {
            CreationWasalElkheerDto created = wasalElkheerService.createWasalElkheer(form, userId);
            return ResponseEntity.ok(created);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<WasalElkheerDto> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(wasalElkheerService.confirmOrder(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WasalElkheerDto>> getUserOrders(@PathVariable UUID userId) {
        List<WasalElkheerDto> orders = wasalElkheerService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/charity/{charityId}")
    public ResponseEntity<List<WasalElkheerDto>> getCharityOrders(@PathVariable Long charityId) {
        List<WasalElkheerDto> orders = wasalElkheerService.getCharityOrders(charityId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<WasalElkheerDto>> getOrdersByStatus(@PathVariable WasalElkheerStatus status) {
        List<WasalElkheerDto> orders = wasalElkheerService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<WasalElkheerDto> getOrderById(@PathVariable Long orderId) {
        try {
            WasalElkheerDto order = wasalElkheerService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<WasalElkheerDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam WasalElkheerStatus status) {
        try {
            WasalElkheerDto updated = wasalElkheerService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<WasalElkheerDto>> getAllOrders() {
        List<WasalElkheerDto> orders = wasalElkheerService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}