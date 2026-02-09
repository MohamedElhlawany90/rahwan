package com.blueWhale.Rahwan.wasalelkheer;

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
@RequestMapping("/api/wasal-elkheer")
@RequiredArgsConstructor
public class WasalElkheerController {

    private final WasalElkheerService wasalElkheerService;

    /**
     * 1. User: إنشاء طلب جديد
     */
    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreationWasalElkheerDto> createOrder(
            @AuthenticationPrincipal UserPrincipal principal, // 🔐 JWT
            @Valid @ModelAttribute WasalElkheerForm form
    ) throws IOException {

        CreationWasalElkheerDto created =
                wasalElkheerService.createWasalElkheer(form, principal.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    /**
     * 2. User: تأكيد الطلب
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
     * 3. Driver: قبول الطلب
     */
    @PostMapping("/{orderId}/confirm-by-driver")
    public ResponseEntity<WasalElkheerDto> driverConfirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.driverConfirmOrder(orderId, principal.getId())
        );
    }

    /**
     * 4. User: تعديل الطلب
     */
    @PutMapping(
            value = "/update/{orderId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreationWasalElkheerDto> updateOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal, // 🔐
            @ModelAttribute WasalElkheerForm form
    ) throws IOException {

        return ResponseEntity.ok(
                wasalElkheerService.updateOrder(orderId, form, principal.getId())
        );
    }

    /**
     * 5. Driver: تأكيد الاستلام OTP
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
     * 6. Driver: تحديث الطلب "في الطريق"
     */
    @PatchMapping("/{orderId}/in-the-way")
    public ResponseEntity<WasalElkheerDto> updateToInTheWay(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.updateToInTheWay(orderId)
        );
    }

    /**
     * 7. Driver: تأكيد التسليم OTP
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
     * 8. Driver: إرجاع الطلب
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<WasalElkheerDto> returnOrder(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.returnOrder(orderId)
        );
    }

    /**
     * 9. User: جلب طلبات المستخدم
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WasalElkheerDto>> getUserOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                wasalElkheerService.getUserOrders(userId)
        );
    }

    /**
     * 10. Charity: جلب طلبات الجمعية
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
     * 11. جلب الطلبات المتاحة للسائق
     */
    @GetMapping("/available")
    public ResponseEntity<List<WasalElkheerDto>> getAvailableOrders() {
        return ResponseEntity.ok(
                wasalElkheerService.getAvailableOrders()
        );
    }

    /**
     * 12. جلب الطلبات حسب الحالة
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
     * 13. جلب تفاصيل طلب
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
     * 14. Admin: جلب كل الطلبات
     */
    @GetMapping
    public ResponseEntity<List<WasalElkheerDto>> getAllOrders() {
        return ResponseEntity.ok(
                wasalElkheerService.getAllOrders()
        );
    }

    /**
     * 15. Admin: تغيير حالة الطلب
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
}
