package com.truthlens.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Builder.Default
    private String role = "ROLE_USER"; // ROLE_USER, ROLE_ADMIN

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, WARNED, BANNED

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
