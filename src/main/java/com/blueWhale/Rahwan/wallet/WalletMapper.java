package com.blueWhale.Rahwan.wallet;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "userId", expression = "java(wallet.getUser().getId().toString())")
    @Mapping(target = "userName", expression = "java(wallet.getUser().getName())")
    @Mapping(target = "totalBalance", expression = "java(wallet.getWalletBalance() + wallet.getFrozenBalance())")
    WalletDto toDto(Wallet wallet);
}