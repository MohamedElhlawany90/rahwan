package com.blueWhale.Rahwan.charity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CharityMapper {

    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "active", constant = "true")
    Charity toEntity(CharityForm form);

    CharityDto toDto(Charity charity);
}