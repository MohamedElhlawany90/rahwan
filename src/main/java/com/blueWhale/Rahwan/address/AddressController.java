package com.blueWhale.Rahwan.address;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * Create Address (Pickup)
     */
    @PostMapping("/pickup")
    public AddressDto createPickup(
            @RequestBody AddressForm form,
            @RequestParam UUID userId
    ) {
        return addressService.create(form, userId);
    }

    /**
     * Create Address (Dropoff)
     */
    @PostMapping("/recipient")
    public AddressDto createDropoff(
            @RequestBody AddressForm form,
            @RequestParam UUID userId
    ) {
        return addressService.create(form, userId);
    }

    /**
     * Update Pickup Location (Map)
     */
    @PutMapping("/{id}/pickup")
    public AddressDto updatePickup(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return addressService.updatePickupLocation(id, lat, lng);
    }

    /**
     * Update Recipient Location (Manual Input)
     */
    @PutMapping("/{id}/recipient")
    public AddressDto updateRecipient(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return addressService.updateRecipientLocation(id, lat, lng);
    }

    @GetMapping("/{id}")
    public AddressDto get(@PathVariable Long id) {
        return addressService.getById(id);
    }

    @GetMapping("/pickup/{userId}")
    public List<AddressDto> getUserPickupAddresses(@PathVariable UUID userId) {
        return addressService.getUserAddresses(userId);
    }

    @GetMapping("/recipient/{userId}")
    public List<AddressDto> getUserRecipientAddresses(@PathVariable UUID userId) {
        return addressService.getUserAddresses(userId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        addressService.delete(id);
    }
}
