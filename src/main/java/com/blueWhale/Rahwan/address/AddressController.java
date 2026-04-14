package com.blueWhale.Rahwan.address;

import com.blueWhale.Rahwan.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * Create Pickup Address
     */
    @PostMapping("/pickup")
    public ResponseEntity<PickupAddressDto> createPickup(
            @AuthenticationPrincipal UserPrincipal principal, // 🔐 JWT
            @Valid @RequestBody PickupAddressForm form
    ) {
        UUID userId = principal.getId(); // 🔐
        return ResponseEntity.ok(
                addressService.createPickup(form, userId)
        );
    }

    /**
     * Create Dropoff Address
     */
    @PostMapping("/recipient")
    public ResponseEntity<DropoffAddressDto> createDropoff(
            @AuthenticationPrincipal UserPrincipal principal, // 🔐 JWT
            @Valid @RequestBody DropoffAddressForm form
    ) {
        UUID userId = principal.getId(); // 🔐
        return ResponseEntity.ok(
                addressService.createDropoff(form, userId)
        );
    }

    /**
     * Update Pickup Location
     */
    @PutMapping("/{id}/pickup")
    public ResponseEntity<PickupAddressDto> updatePickup(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return ResponseEntity.ok(
                addressService.updatePickupLocation(id, lat, lng)
        );
    }

    /**
     * Update Dropoff Location
     */
    @PutMapping("/{id}/recipient")
    public ResponseEntity<DropoffAddressDto> updateRecipient(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return ResponseEntity.ok(
                addressService.updateRecipientLocation(id, lat, lng)
        );
    }

    /**
     * Get Address By id
     */
    @GetMapping("/{id}")
    public ResponseEntity<AddressDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                addressService.getById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<AddressDto>> getAllAddresses(
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                addressService.getAllAddresses(principal.getId())
        );
    }

    /**
     * Get User Pickup Addresses
     */
    @GetMapping("/pickup")
    public ResponseEntity<List<PickupAddressDto>> getUserPickupAddresses(
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                addressService.getUserPickupAddresses(principal.getId())
        );
    }

    /**
     * Get User Dropoff Addresses
     */
    @GetMapping("/recipient")
    public ResponseEntity<List<DropoffAddressDto>> getUserRecipientAddresses(
            @AuthenticationPrincipal UserPrincipal principal // 🔐
    ) {
        return ResponseEntity.ok(
                addressService.getUserDropoffAddresses(principal.getId())
        );
    }

    /**
     * Delete Address
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
