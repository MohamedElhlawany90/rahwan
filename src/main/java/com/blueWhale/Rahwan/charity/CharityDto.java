package com.blueWhale.Rahwan.charity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharityDto {

    private Long id;
    private String nameAr;
    private String nameEn;
    private String descriptionAr;
    private String descriptionEn;
    private String phone;
    private Double latitude;
    private Double longitude;
    private String logo;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}