package com.blueWhale.Rahwan.address;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository, AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.addressMapper = addressMapper;
    }

    public PickupAddressDto createPickup(PickupAddressForm pickupAddressForm, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = addressMapper.fromForm(pickupAddressForm);
        address.setUser(user);

        // ✅ FIX: Removed the redundant manual setPickuplatitude/setPickuplongitude calls.
        // The mapper's @Mapping annotations already handle copying these fields from the form.
        // Calling set() again after the mapper was just harmless noise but violated DRY.

        return addressMapper.toPickupDto(addressRepository.save(address));
    }

    public DropoffAddressDto createDropoff(DropoffAddressForm addressForm, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = addressMapper.fromForm(addressForm);
        address.setUser(user);

        return addressMapper.toDropoffDto(addressRepository.save(address));
    }

    public List<PickupAddressDto> getUserPickupAddresses(UUID userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toPickupDto)
                .toList();
    }

    public List<DropoffAddressDto> getUserDropoffAddresses(UUID userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toDropoffDto)
                .toList();
    }

    public List<AddressDto> getAllAddresses(UUID userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toDto)
                .toList();
    }

    public PickupAddressDto updatePickupLocation(Long addressId, double pickupLat, double pickupLng) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setPickuplatitude(pickupLat);
        address.setPickuplongitude(pickupLng);

        return addressMapper.toPickupDto(address);
    }

    public DropoffAddressDto updateRecipientLocation(Long addressId, double recipientLat, double recipientLng) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setDropOfflatitude(recipientLat);
        address.setDropOfflongitude(recipientLng);

        return addressMapper.toDropoffDto(address);
    }

    public AddressDto getById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return addressMapper.toDto(address);
    }

    public void delete(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }
}