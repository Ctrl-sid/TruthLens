package com.truthlens.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "verified_sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiedSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 150)
    private String domain;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer credibilityScore; // 0 to 100

    @Column(nullable = false, length = 50)
    private String category; // News Agency, FactChecker, Government, Scientific

    private String biasRating; // Center, Slight Left, Slight Right
    
    private String verifiedUrl;
}
