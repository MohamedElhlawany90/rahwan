package com.blueWhale.Rahwan.wasalelkheer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WasalElkheerMapper {

    @Mapping(target = "photo", ignore = true)
    @Mapping(target = "userId", ignore = true)
    WasalElkheer toEntity(WasalElkheerForm form);

    WasalElkheerDto toDto(WasalElkheer WasalElkheer);
}