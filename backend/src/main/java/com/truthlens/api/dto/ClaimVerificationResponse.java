package com.truthlens.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimVerificationResponse {
    private Long id;
    private String inputType;
    private String claimSummary;
    private int genuinenessScore; // 0 to 100
    private String verdict; // VERIFIED GENUINE, MOSTLY GENUINE, MIXED / UNVERIFIED, INSUFFICIENT_EVIDENCE, LIKELY FAKE, FABRICATED
    private String verdictBadgeColor; // #10B981, #F59E0B, #94A3B8, #EF4444
    private String confidence; // HIGH, MEDIUM, LOW
    private String rationale;
    private List<String> keyReasons;
    @Builder.Default
    private List<DecomposedClaim> subClaims = new ArrayList<>();
    @Builder.Default
    private List<EvidenceCluster> evidenceClusters = new ArrayList<>();
    @Builder.Default
    private List<SourceEvidence> sources = new ArrayList<>();
    private ExplainabilityProfile explainability;
    private ContentCharacteristics contentDiagnostics;
    private ClaimOriginDiscovery originDiscovery;
    private NlpAnalysisResponse nlpAnalysis;
    private ImageIntegrityAnalysis imageAnalysis; // Null if not image input
    private String timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DecomposedClaim {
        private String claimText;
        private String claimType; // EVENT_OCCURRENCE, CASUALTY_COUNT, TEMPORAL_DATE, LOCATION_FACT, ATTRIBUTION
        private int claimScore; // 0 to 100
        private String claimVerdict; // VERIFIED, REFUTED, UNVERIFIED, INSUFFICIENT_EVIDENCE
        private String stance; // SUPPORTED, REFUTED, CONFIRMED, DENIED, UNCERTAIN
        private String evidenceSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceCluster {
        private String clusterId;
        private String clusterTheme;
        private String primaryOutlet;
        private List<String> affiliatedOutlets;
        private int sourceCount;
        private double independenceRating; // e.g. 100% for primary agency, 40% for syndicated republishers
        private String consensusStance; // SUPPORTED, REFUTED, CONTRADICTED, UNCERTAIN
        private String evidenceTier; // LEVEL_1_PRIMARY, LEVEL_2_SECONDARY, LEVEL_3_FACTCHECK, LEVEL_4_REFERENCE, LEVEL_5_USER_GENERATED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExplainabilityProfile {
        private String confidenceLevel; // HIGH, MEDIUM, LOW
        @Builder.Default
        private List<String> positiveChecklist = new ArrayList<>(); // e.g. "✓ Corroborated across 2 independent wire clusters"
        @Builder.Default
        private List<String> warningChecklist = new ArrayList<>(); // e.g. "⚠ Numerical discrepancy: claim states 0, wire confirms 166"
        @Builder.Default
        private List<String> detectedDifferences = new ArrayList<>();
        @Builder.Default
        private List<EvidenceItemSummary> evidenceMatrix = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceItemSummary {
        private String sourceName;
        private String sourceType; // Primary Government, News Wire, Fact-Check, Reference, Social
        private String evidenceTier; // LEVEL 1 to LEVEL 5
        private String stance; // SUPPORTED, REFUTED, CONTRADICTED, UNCERTAIN
        private String reliability; // HIGH, MEDIUM, LOW
        private double independence; // 0% to 100%
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContentCharacteristics {
        private String sensationalismLevel; // LOW, MEDIUM, HIGH
        private int clickbaitRating; // 0 to 100
        private String subjectivityLevel; // OBJECTIVE, BALANCED, SUBJECTIVE
        private String emotionalTone; // NEUTRAL, SENSATIONAL, PANIC_INDUCING
        private List<String> triggerFlags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimOriginDiscovery {
        private String originalPublisher; // e.g. "The Hindu", "Reuters", "FDA", "Snopes"
        private String originalDomain; // e.g. "thehindu.com"
        private String originalHeadline; // Exact original article title
        private String originalUrl; // Direct link
        private String publishedDate; // Publication date if available
        private String provenanceType; // AUTHENTIC_REPRODUCTION, ALTERED_DISTORTION, DOCUMENTED_HOAX, UNVERIFIED_ORIGIN
        private String provenanceStatus; // PRIMARY_SOURCE_FOUND, ORIGINAL_REPORT_FOUND, SECONDARY_REPORT_FOUND, MULTIPLE_RELATED_SOURCES_FOUND, ORIGIN_NOT_DETERMINED
        private String evidenceTier; // LEVEL_1_PRIMARY, LEVEL_2_SECONDARY, LEVEL_3_FACTCHECK, LEVEL_4_REFERENCE
        private String distortionAnalysis; // Details of what was altered/manipulated
        private String crossReferencedConsensus; // e.g. "Corroborated across 3 Tier-1 wire agencies"
        private double originMatchConfidence; // 0.0 to 100.0%
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceEvidence {
        private String sourceName;
        private String domain;
        private String evidenceTier; // LEVEL_1_PRIMARY, LEVEL_2_SECONDARY, LEVEL_3_FACTCHECK, LEVEL_4_REFERENCE, LEVEL_5_USER_GENERATED
        private int credibilityRating;
        private double matchPercentage;
        private double independenceRating; // 0 to 100%
        private String stance; // SUPPORTED, DENIED, CONFIRMED, REFUTED, UNCERTAIN
        private String verdictBySource; // True, False, Unverified
        private String articleTitle;
        private String url;
        private String clusterId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageIntegrityAnalysis {
        private String detectedHeadlineText;
        private double manipulationProbability; // 0% to 100%
        private String manipulationVerdict; // "Potential Image Manipulation Indicators", "Clean Visual Grid"
        private String imageContextStatus; // "Context Matches Claim", "Misleading / Repurposed Visual Context", "Unverified Visual"
        private String exifStatus; // "Clean EXIF", "Stripped / Edited Metadata"
        private List<String> anomalyFlags; // ["Noise Variance Artifacts", "Text Overlay Inconsistency"]
        private String heatmapOverlayUrl;
    }
}
