// ============================================
// UserMapper.java (Updated)
// ============================================
package com.blueWhale.Rahwan.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    User toEntity(UserForm form);


    UserDto toDto(User user);
}