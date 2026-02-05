package com.blueWhale.Rahwan.commission;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "commission_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String settingKey = "DEFAULT_COMMISSION";

    @Column(nullable = false)
    private double commissionRate = 10.0; // نسبة العمولة %

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}