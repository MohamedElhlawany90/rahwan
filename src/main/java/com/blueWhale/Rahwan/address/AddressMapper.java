package com.blueWhale.Rahwan.address;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "pickuplatitude", source = "pickuplatitude")
    @Mapping(target = "pickuplongitude", source = "pickuplongitude")
    Address fromForm(PickupAddressForm form);

    @Mapping(target = "dropOfflatitude", source = "dropOfflatitude")
    @Mapping(target = "dropOfflongitude", source = "dropOfflongitude")
    Address fromForm(DropoffAddressForm form);

    PickupAddressDto toPickupDto(Address address);

    DropoffAddressDto toDropoffDto(Address address);

    @Mapping(target = "pickuplatitude", source = "pickuplatitude")
    @Mapping(target = "pickuplongitude", source = "pickuplongitude")
    @Mapping(target = "dropOfflatitude", source = "dropOfflatitude")
    @Mapping(target = "dropOfflongitude", source = "dropOfflongitude")
    AddressDto toDto(Address address);
}

