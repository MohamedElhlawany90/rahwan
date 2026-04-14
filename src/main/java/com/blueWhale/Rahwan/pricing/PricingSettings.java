package com.blueWhale.Rahwan.pricing;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String settingKey = "DEFAULT_PRICING";

    @Column(nullable = false)
    private double baseCost = 20.0;

    @Column(nullable = false)
    private double costPerKm = 2.5;

    @Column(nullable = false)
    private double roadMultiplier = 1.2;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
