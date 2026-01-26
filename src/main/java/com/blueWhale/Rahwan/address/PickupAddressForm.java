package com.blueWhale.Rahwan.address;


import lombok.Data;

@Data
public class PickupAddressForm {

    private double pickuplatitude;
    private double pickuplongitude;

    private String governerate ;
    private String blockNumber ;
    private String apartmentNumber ;
    private String streetName ;
}
