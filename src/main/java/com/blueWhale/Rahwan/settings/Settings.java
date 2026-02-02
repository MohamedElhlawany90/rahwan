package com.blueWhale.Rahwan.settings;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "settings")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime updatedDate = LocalDateTime.now();

    private String createdBy;
    private Long updatedBy;

    @Column(columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(columnDefinition = "TEXT")
    private String shippingTerms;

    private String requestType;
    private String rejectReason;

    @Column(columnDefinition = "TEXT")
    private String contactUs;
}
