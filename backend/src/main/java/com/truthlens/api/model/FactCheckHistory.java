package com.truthlens.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fact_check_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactCheckHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 20)
    private String inputType; // TEXT, URL, IMAGE

    @Column(columnDefinition = "TEXT", nullable = false)
    private String inputContent;

    private String claimSummary;

    @Column(nullable = false)
    private Integer genuinenessScore; // 0 to 100

    @Column(nullable = false, length = 50)
    private String verdict; // GENUINE, MOSTLY_GENUINE, MIXED_MISLEADING, LIKELY_FAKE, FABRICATED

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rationale;

    @Column(columnDefinition = "TEXT")
    private String nlpMetricsJson; // Serialized JSON of sentiment, entities, clickbait score

    @Column(columnDefinition = "TEXT")
    private String sourceEvidenceJson; // Serialized JSON of sources & citations

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
