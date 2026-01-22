package com.blueWhale.Rahwan.wasalelkheer;

import com.blueWhale.Rahwan.order.CreationDto;
import com.blueWhale.Rahwan.order.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WasalElkheerMapper {

    @Mapping(target = "photo", ignore = true)
    @Mapping(target = "userId", ignore = true)
    WasalElkheer toEntity(WasalElkheerForm form);

    WasalElkheerDto toDto(WasalElkheer WasalElkheer);

    @Mapping(source = "photo", target = "photo")
    @Mapping(source = "creationStatus", target = "status")
    CreationWasalElkheerDto toCreationWasalDto(WasalElkheer wasalElkheer);
}