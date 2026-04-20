package com.blueWhale.Rahwan.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", ignore = true)   // بنحطها يدوياً في الـ Service
    @Mapping(target = "wallet", ignore = true)
    User toEntity(UserForm form);

    @Mapping(target = "roles", ignore = true)   // بنحطها يدوياً في الـ Service
    @Mapping(target = "wallet", ignore = true)
    User toEntity(DriverForm form);

    @Mapping(target = "walletBalance", expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() : 0.0)")
    @Mapping(target = "frozenBalance",  expression = "java(user.getWallet() != null ? user.getWallet().getFrozenBalance() : 0.0)")
    @Mapping(target = "totalBalance",   expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() + user.getWallet().getFrozenBalance() : 0.0)")
    UserDto toDto(User user);

    @Mapping(target = "walletBalance", expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() : 0.0)")
    @Mapping(target = "frozenBalance",  expression = "java(user.getWallet() != null ? user.getWallet().getFrozenBalance() : 0.0)")
    @Mapping(target = "totalBalance",   expression = "java(user.getWallet() != null ? user.getWallet().getWalletBalance() + user.getWallet().getFrozenBalance() : 0.0)")
    SignInDto toSignInDto(User user);
}