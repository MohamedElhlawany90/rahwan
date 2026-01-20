package com.blueWhale.Rahwan.charity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "charities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nameAr;

    @Column(nullable = false)
    private String nameEn;

    @Column(columnDefinition = "TEXT")
    private String descriptionAr;

    @Column(columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(nullable = false, unique = true, length = 12)
    private String phone;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String logo;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}