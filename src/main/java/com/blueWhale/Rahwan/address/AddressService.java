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

    /**
     * Create Address (Pickup + Dropoff)
     */
    public AddressDto create(AddressForm addressForm, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = addressMapper.fromForm(addressForm);
        address.setUser(user);


        Address saved = addressRepository.save(address);
        return addressMapper.toDto(saved);
    }
    /**
     * Get all addresses for user
     */
    public List<AddressDto> getUserAddresses(UUID userId) {

        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toDto)
                .toList();
    }

    /**
     * Update Pickup Location only (User)
     */
    public AddressDto updatePickupLocation(Long addressId, double pickupLat, double pickupLng) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setLatitude(pickupLat);
        address.setLongitude(pickupLng);

        return addressMapper.toDto(address);
    }

    /**
     * Update Dropoff Location only (Recipient)
     */
    public AddressDto updateRecipientLocation(
            Long addressId,
            double recipientLat,
            double recipientLng
    ) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setLatitude(recipientLat);
        address.setLongitude(recipientLng);

        return addressMapper.toDto(address);
    }

    /**
     * Get Address by ID
     */
    public AddressDto getById(Long addressId) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        return addressMapper.toDto(address);
    }

    /**
     * Delete Address
     */
    public void delete(Long addressId) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        addressRepository.delete(address);
    }
}

