// ============================================
// UserMapper.java (Updated)
// ============================================
package com.blueWhale.Rahwan.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    User toEntity(UserForm form);


    @Mapping(target = "walletBalance", expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() : 0.0)")
    @Mapping(target = "frozenBalance", expression = "java(user.getWallet() != null ? user.getWallet().getFrozenBalance() : 0.0)")
    @Mapping(target = "totalBalance", expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() + user.getWallet().getFrozenBalance() : 0.0)")
    UserDto toDto(User user);


    @Mapping(target = "walletBalance", expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() : 0.0)")
    @Mapping(target = "frozenBalance", expression = "java(user.getWallet() != null ? user.getWallet().getFrozenBalance() : 0.0)")
    @Mapping(target = "totalBalance", expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() + user.getWallet().getFrozenBalance() : 0.0)")
    SignInDto toSignInDto(User user);
}