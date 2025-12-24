// ============================================
// UserMapper.java (Updated)
// ============================================
package com.blueWhale.Rahwan.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "active", constant = "true")
//    @Mapping(target = "wallet", ignore = true)
//    @Mapping(target = "otpPhone", ignore = true)
//    @Mapping(target = "verifiedPhone", constant = "false")
//    @Mapping(target = "profileImage", ignore = true)
    User toEntity(UserForm form);

//    @Mapping(target = "createdAt", source = "createdAt")
//    @Mapping(target = "walletBalance", source = "wallet.balance")
    UserDto toDto(User user);
}