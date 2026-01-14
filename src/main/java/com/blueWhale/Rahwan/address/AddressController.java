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
    public PickupAddressDto createPickup(
            @RequestBody PickupAddressForm form,
            @RequestParam UUID userId
    ) {
        return addressService.createPickup(form, userId);
    }

    /**
     * Create Address (Dropoff)
     */
    @PostMapping("/recipient")
    public DropoffAddressDto createDropoff(
            @RequestBody DropoffAddressForm form,
            @RequestParam UUID userId
    ) {
        return addressService.createDropoff(form, userId);


    }

    /**
     * Update Pickup Location (Map)
     */
    @PutMapping("/{id}/pickup")
    public PickupAddressDto updatePickup(
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
    public DropoffAddressDto updateRecipient(
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
    public List<PickupAddressDto> getUserPickupAddresses(@PathVariable UUID userId) {
        return addressService.getUserPickupAddresses(userId);
    }

    @GetMapping("/recipient/{userId}")
    public List<DropoffAddressDto> getUserRecipientAddresses(@PathVariable UUID userId) {
        return addressService.getUserDropoffAddresses(userId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        addressService.delete(id);
    }
}
