package com.blueWhale.Rahwan.address;

import lombok.Data;

@Data
public class AddressDto {

    private Long id;

    private double pickuplatitude;
    private double pickuplongitude;

    private double dropOfflatitude;
    private double dropOfflongitude;

    private String governerate ;
    private String blockNumber ;
    private String apartmentNumber ;
    private String streetName ;


}
