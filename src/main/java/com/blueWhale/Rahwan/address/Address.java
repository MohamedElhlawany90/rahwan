package com.blueWhale.Rahwan.address;

import com.blueWhale.Rahwan.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double pickuplatitude;
    private double pickuplongitude;

    private double dropOfflatitude;
    private double dropOfflongitude;

    @ManyToOne
    private User user ;
}