package com.blueWhale.Rahwan.location;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue
    private UUID id;

    private double latitude;
    private double longitude;
    private String address;

    @Enumerated(EnumType.STRING)
    private LocationType type;
}
