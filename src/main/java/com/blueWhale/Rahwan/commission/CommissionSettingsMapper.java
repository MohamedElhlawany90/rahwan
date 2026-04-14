package com.blueWhale.Rahwan.commission;


import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommissionSettingsMapper {

    CommissionSettings toEntity(CommissionSettingsForm form);

    CommissionSettingsDto toDto(CommissionSettings settings);
}