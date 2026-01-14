package com.blueWhale.Rahwan.address;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {

    Address fromForm(AddressForm form);

    PickupAddressDto toPickupDto(Address address);

    DropoffAddressDto toDropoffDto(Address address);

    AddressDto toDto(Address address);
}

