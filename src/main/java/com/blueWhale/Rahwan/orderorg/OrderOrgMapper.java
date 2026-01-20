package com.blueWhale.Rahwan.orderorg;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderOrgMapper {

    @Mapping(target = "photo", ignore = true)
    @Mapping(target = "userId", ignore = true)
    OrderOrg toEntity(OrderOrgForm form);

    OrderOrgDto toDto(OrderOrg orderOrg);
}