package com.blueWhale.Rahwan.advertisement;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "advertisements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String photo;

    private String name;

    private Boolean isShown;

    private String note;
}
