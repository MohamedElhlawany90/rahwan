package com.blueWhale.Rahwan.advertisement;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AdvertisementMapper {

    @Mapping(target = "photo", ignore = true)
    Advertisement toEntity(AdvertisementForm form);

    AdvertisementDto toDto(Advertisement entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "photo", ignore = true)
    void updateEntity(AdvertisementForm form, @MappingTarget Advertisement entity);
}
