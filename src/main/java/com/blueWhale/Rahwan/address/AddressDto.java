package com.blueWhale.Rahwan.address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private Long id;

    private double latitude;
    private double longitude;

//    private UUID userId;

}