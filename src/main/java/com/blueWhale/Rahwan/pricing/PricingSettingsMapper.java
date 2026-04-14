package com.blueWhale.Rahwan.pricing;


import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PricingSettingsMapper {

    PricingSettings toEntity(PricingSettingsForm form);

    PricingSettingsDto toDto(PricingSettings settings);
}