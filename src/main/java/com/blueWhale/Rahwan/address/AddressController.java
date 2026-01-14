package com.blueWhale.Rahwan.address;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;
    private final AddressMapper addressMapper;

    public AddressController(AddressService addressService, AddressMapper addressMapper) {
        this.addressService = addressService;
        this.addressMapper = addressMapper;
    }

    /**
     * Create Address (Pickup)
     */
    @PostMapping("/pickup")
    public PickupAddressDto createPickup(
            @RequestBody AddressForm form,
            @RequestParam UUID userId
    ) {
        Address saved = addressService.create(form, userId);
        return addressMapper.toPickupDto(saved);
    }

    /**
     * Create Address (Dropoff)
     */
    @PostMapping("/recipient")
    public DropoffAddressDto createDropoff(
            @RequestBody AddressForm form,
            @RequestParam UUID userId
    ) {
        Address saved = addressService.create(form, userId);

        return addressMapper.toDropoffDto(saved);
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
