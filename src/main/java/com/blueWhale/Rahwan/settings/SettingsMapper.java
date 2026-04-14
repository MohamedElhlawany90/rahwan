package com.blueWhale.Rahwan.settings;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SettingsMapper {

    Settings toEntity(SettingsForm form);

    SettingsDto toDto(Settings settings);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromForm(SettingsForm form, @MappingTarget Settings settings);
}
