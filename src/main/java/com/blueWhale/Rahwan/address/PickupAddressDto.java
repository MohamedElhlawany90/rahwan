package com.blueWhale.Rahwan.address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickupAddressDto {
    private Long id;
    private double pickuplatitude;
    private double pickuplongitude;

    private String governerate ;
    private String blockNumber ;
    private String apartmentNumber ;
    private String streetName ;
}
