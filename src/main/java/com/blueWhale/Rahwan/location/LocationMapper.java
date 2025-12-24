package com.blueWhale.Rahwan.location;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    LocationDto toDto(Location location);
}
