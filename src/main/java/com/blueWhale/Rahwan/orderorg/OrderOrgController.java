package com.blueWhale.Rahwan.orderorg;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
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
@RequestMapping("/api/order-org")
@RequiredArgsConstructor
public class OrderOrgController {

    private final OrderOrgService orderOrgService;

    @PostMapping(value = "/create/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrderOrgDto> createOrder(
            @PathVariable UUID userId,
            @Valid @ModelAttribute OrderOrgForm form) {
        try {
            OrderOrgDto created = orderOrgService.createOrderOrg(form, userId);
            return ResponseEntity.ok(created);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderOrgDto>> getUserOrders(@PathVariable UUID userId) {
        List<OrderOrgDto> orders = orderOrgService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/charity/{charityId}")
    public ResponseEntity<List<OrderOrgDto>> getCharityOrders(@PathVariable Long charityId) {
        List<OrderOrgDto> orders = orderOrgService.getCharityOrders(charityId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderOrgDto>> getOrdersByStatus(@PathVariable OrderOrgStatus status) {
        List<OrderOrgDto> orders = orderOrgService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderOrgDto> getOrderById(@PathVariable Long orderId) {
        try {
            OrderOrgDto order = orderOrgService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderOrgDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderOrgStatus status) {
        try {
            OrderOrgDto updated = orderOrgService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderOrgDto>> getAllOrders() {
        List<OrderOrgDto> orders = orderOrgService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}