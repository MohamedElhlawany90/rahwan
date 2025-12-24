package com.blueWhale.Rahwan.advertisement;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdvertisementDto {

    private String id;
    private String photo; // URL أو path
    private String name;
    private Boolean isShown;
    private String note;
}
