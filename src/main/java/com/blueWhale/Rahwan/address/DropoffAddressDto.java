package com.blueWhale.Rahwan.address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropoffAddressDto {

    private Long id;
    private double dropOfflatitude;
    private double dropOfflongitude;
}
