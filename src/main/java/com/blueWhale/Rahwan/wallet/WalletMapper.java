package com.blueWhale.Rahwan.wallet;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(
            target = "userId",
            expression = "java(wallet.getUser().getId().toString())"
    )
    WalletDto toDto(Wallet wallet);
}
