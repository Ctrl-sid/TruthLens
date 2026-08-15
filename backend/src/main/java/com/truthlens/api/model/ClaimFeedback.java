package com.truthlens.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private FactCheckHistory claim;

    private Integer rating; // 1 to 5 stars

    @Column(length = 100)
    private String flagReason; // INACCURATE_FACT, CULTURALLY_INAPPROPRIATE, PERSONAL_BIAS, OTHER

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
