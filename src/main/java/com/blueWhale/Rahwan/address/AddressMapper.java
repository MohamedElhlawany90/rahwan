package com.blueWhale.Rahwan.address;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {


//    @Mapping(target = "user.id", source = "userId")
    Address fromForm(AddressForm addressForm);

//    @Mapping(target = "userId", source = "user.id")
    AddressDto toDto (Address address);


}
