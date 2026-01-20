package com.blueWhale.Rahwan.order;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "photo", ignore = true)
    Order toEntity(OrderForm form);

    @Mapping(source = "photo", target = "photo")
    OrderDto toDto(Order order);

    @Mapping(source = "photo", target = "photo")
    @Mapping(source = "creationStatus", target = "status")
    CreationDto toCreationDto(Order order);
}
