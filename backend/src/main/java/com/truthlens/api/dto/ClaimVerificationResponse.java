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
    private Integer genuinenessScore; // Nullable for NOT_VERIFIABLE (rendered as N/A), 0 to 100 otherwise
    private String verdict; // VERIFIED GENUINE, MOSTLY GENUINE, MIXED / CONFLICTING, INSUFFICIENT_EVIDENCE, STRONGLY CONTRADICTED, DOCUMENTED_HOAX, NOT_VERIFIABLE
    private String verdictBadgeColor; // #10B981, #F59E0B, #94A3B8, #EF4444, #64748B
    private String confidence; // HIGH, MEDIUM, LOW
    private Integer confidenceScore; // 0 to 100%
    private String contradictionSeverity; // NONE, MINOR_DISCREPANCY, MODERATE_CONTRADICTION, MAJOR_CONTRADICTION, DIRECT_FACTUAL_REVERSAL
    private String failureState; // NONE, OCR_FAILED, URL_UNREACHABLE, NO_RELEVANT_EVIDENCE, INSUFFICIENT_EVIDENCE, SOURCE_CONFLICT, VERIFICATION_TIMEOUT
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
    @Builder.Default
    private String algorithmVersion = "2.1";
    @Builder.Default
    private String scoringVersion = "2.1";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DecomposedClaim {
        private String claimText;
        private String claimType; // EVENT_OCCURRENCE, CASUALTY_COUNT, TEMPORAL_DATE, LOCATION_FACT, ATTRIBUTION, QUANTITY, STATISTICAL_CLAIM
        private String claimCentrality; // PRIMARY_CLAIM, SUPPORTING_CLAIM, MINOR_CLAIM
        private double claimImportanceWeight; // e.g. 0.60 for Primary, 0.30 for Supporting, 0.10 for Minor
        private Integer claimScore; // 0 to 100
        private String claimVerdict; // VERIFIED, MOSTLY_VERIFIED, REFUTED, UNVERIFIED, INSUFFICIENT_EVIDENCE
        private String stance; // SUPPORTED, CONFIRMED, ARTICLE_REPORTS_CLAIM, REFUTED, DENIED, PARTIALLY_SUPPORTED, NOT_MENTIONED, UNCERTAIN
        private String evidenceSummary;
        private boolean isNegated;
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
        private String consensusStance; // SUPPORTED, CONFIRMED, ARTICLE_REPORTS_CLAIM, REFUTED, CONTRADICTED, UNCERTAIN
        private String evidenceTier; // LEVEL_1_PRIMARY, LEVEL_2_SECONDARY, LEVEL_3_FACTCHECK, LEVEL_4_REFERENCE, LEVEL_5_USER_GENERATED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExplainabilityProfile {
        private String confidenceLevel; // HIGH, MEDIUM, LOW
        private Integer confidenceScore; // 0 to 100%
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
        private String sourceType; // Level 1 Primary Gov, Level 2 News Wire, Level 3 Fact Check, Level 4 Reference, Level 5 Social
        private String evidenceTier; // LEVEL_1_PRIMARY to LEVEL_5_USER_GENERATED
        private String stance; // SUPPORTED, CONFIRMED, ARTICLE_REPORTS_CLAIM, REFUTED, DENIED, NOT_MENTIONED, UNCERTAIN
        private String reliability; // HIGH, MEDIUM, LOW
        private double independence; // 0% to 100%
        private String evidenceRole; // "Direct Verification Evidence" vs "Discovery Candidate"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContentCharacteristics {
        private String sensationalismLevel; // LOW, MEDIUM, HIGH
        private int clickbaitRating; // 0 to 100
        private String subjectivityLevel; // OBJECTIVE, BALANCED, SUBJECTIVE
        private String emotionalTone; // NEUTRAL, SENSATIONAL, EMOTIONALLY_CHARGED, PANIC_INDUCING
        private List<String> triggerFlags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimOriginDiscovery {
        private String originalPublisher; // Fallback / Display name
        private String earliestIdentifiedPublisher; // e.g. "The Hindu", "Reuters", "FDA", "Snopes"
        private String originalDomain; // e.g. "thehindu.com"
        private String originalHeadline; // Exact original article title
        private String originalUrl; // Direct link
        private String publishedDate; // Publication date if available
        private String retrievalTimestamp; // Exact retrieval time
        private String provenanceType; // AUTHENTIC_REPRODUCTION, ALTERED_DISTORTION, DOCUMENTED_HOAX, UNVERIFIED_ORIGIN
        private String provenanceStatus; // EARLIEST_RELIABLE_SOURCE_FOUND, SECONDARY_REPORT_FOUND, MULTIPLE_RELATED_SOURCES_FOUND, ORIGIN_NOT_DETERMINED
        private String claimIntegrity; // AUTHENTIC_REPRODUCTION, MINOR_VARIANCE, ALTERED_DISTORTION, FABRICATED_ASSERTION, DOCUMENTED_HOAX
        private String contradictionSeverity; // NONE, MINOR_DISCREPANCY, MODERATE_CONTRADICTION, MAJOR_CONTRADICTION, DIRECT_FACTUAL_REVERSAL
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
        private String stance; // SUPPORTED, CONFIRMED, ARTICLE_REPORTS_CLAIM, REFUTED, DENIED, PARTIALLY_SUPPORTED, NOT_MENTIONED, UNCERTAIN
        private String verdictBySource; // True, False, Unverified, Reported Allegation
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
        private String manipulationVerdict; // "Image Forensic Indicators: Clean Compression Profile" vs "Potential Anomalies Detected"
        private String imageContextStatus; // "Context Matches Claim", "Misleading / Repurposed Visual Context Likely", "Unverified Context"
        private String exifStatus; // "Sensor Metadata Available", "Stripped by Platform (Neutral)", "Edited Metadata"
        private List<String> anomalyFlags; // ["Visual Sensor Alignment Verified", "Standard Compression Matrix Consistency"]
        private String heatmapOverlayUrl;
    }
}
