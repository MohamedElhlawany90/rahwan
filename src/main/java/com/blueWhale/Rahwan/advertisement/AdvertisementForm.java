package com.blueWhale.Rahwan.advertisement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementForm {

    private MultipartFile photo;
    private String name;
    private Boolean isShown;
    private String note;
}
