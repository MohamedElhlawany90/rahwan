package com.blueWhale.Rahwan.orderorg;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "order_org")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long charityId;

    @Column(nullable = false)
    private Double userLatitude;

    @Column(nullable = false)
    private Double userLongitude;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderOrgType orderType;

    private String photo;

    @Column(columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(nullable = false)
    private LocalDate collectionDate;

    @Column(nullable = false)
    private LocalTime collectionTime;

    @Column(nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private boolean anyTime = false;

    @Column(nullable = false)
    private boolean allowInspection = false;

    @Column(nullable = false)
    private boolean shippingPaidByReceiver = false;

//    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderOrgStatus status = OrderOrgStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}