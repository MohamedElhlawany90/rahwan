package com.blueWhale.Rahwan.order;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderMapper {
//
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "userId", ignore = true)
//    @Mapping(target = "driverId", ignore = true)
//    @Mapping(target = "pictureUrl", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
//    @Mapping(target = "deliveryCost", constant = "0.0")
//    @Mapping(target = "distanceKm", constant = "0.0")
//    @Mapping(target = "trackingNumber", ignore = true)
//    @Mapping(target = "otpForPickup", ignore = true)
//    @Mapping(target = "otpForDelivery", ignore = true)
//    @Mapping(target = "pickupConfirmed", constant = "false")
//    @Mapping(target = "deliveryConfirmed", constant = "false")
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "confirmedAt", ignore = true)
//    @Mapping(target = "pickedUpAt", ignore = true)
//    @Mapping(target = "deliveredAt", ignore = true)
    Order toEntity(OrderForm form);

    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "driverName", ignore = true)
    OrderDto toDto(Order order);
}