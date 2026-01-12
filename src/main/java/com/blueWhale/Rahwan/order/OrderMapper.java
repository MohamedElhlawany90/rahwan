package com.blueWhale.Rahwan.order;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderMapper {

@Mapping(target = "photo", ignore = true)
Order toEntity(OrderForm form);


    OrderDto toDto(Order order);
}