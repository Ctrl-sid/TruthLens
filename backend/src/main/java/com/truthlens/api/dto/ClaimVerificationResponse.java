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
    private Integer supportScore; // Explicit alias for Evidence Support Score (0 to 100)
    private String verdict; // VERIFIED / STRONGLY SUPPORTED, MOSTLY SUPPORTED, PARTIALLY SUPPORTED, MIXED / CONFLICTING EVIDENCE, INSUFFICIENT EVIDENCE, DEVELOPING EVENT, OUTDATED / SUPERSEDED, CONTRADICTED, STRONGLY CONTRADICTED, NON-VERIFIABLE INPUT
    private String verdictBadgeColor; // #10B981, #F59E0B, #94A3B8, #EF4444, #64748B
    private String confidence; // HIGH, MEDIUM, LOW
    private Integer confidenceScore; // 0 to 100%
    private Integer evidenceCompleteness; // 0 to 100% (e.g. 3/4 verified sub-claims = 75%)
    private String asOfStatus; // SUPPORTED_AT_CLAIM_TIME, CURRENTLY_VALID, OUTDATED_SUPERSEDED, UNVERIFIED
    private String distortionType; // NUMERICAL_DISTORTION, LOCATION_DISTORTION, ENTITY_DISTORTION, ATTRIBUTION_DISTORTION, POLARITY_DISTORTION, CONTEXT_DISTORTION, OMISSION_DISTORTION, NONE
    private String contradictionSeverity; // NONE, MINOR_DISCREPANCY, MODERATE_CONTRADICTION, MAJOR_CONTRADICTION, DIRECT_FACTUAL_REVERSAL
    private String failureState; // NONE, OCR_FAILED, URL_UNREACHABLE, NO_RELEVANT_EVIDENCE, INSUFFICIENT_EVIDENCE, SOURCE_CONFLICT, VERIFICATION_TIMEOUT
    private String rationale;
    private List<String> keyReasons;
    private ClaimContextInfo claimContext;
    private RetrievalAudit retrievalAudit;
    @Builder.Default
    private List<DecomposedClaim> subClaims = new ArrayList<>();
    @Builder.Default
    private List<EvidenceCluster> evidenceClusters = new ArrayList<>();
    @Builder.Default
    private List<SourceEvidence> sources = new ArrayList<>();
    @Builder.Default
    private List<PipelineStep> pipelineSteps = new ArrayList<>();
    private ExplainabilityProfile explainability;
    private ContentCharacteristics contentDiagnostics;
    private ClaimOriginDiscovery originDiscovery;
    private NlpAnalysisResponse nlpAnalysis;
    private ImageIntegrityAnalysis imageAnalysis; // Null if not image input
    private String timestamp;
    @Builder.Default
    private String algorithmVersion = "2.3";
    @Builder.Default
    private String scoringVersion = "2.3";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PipelineStep {
        private String stepNumber; // e.g. "01", "02"
        private String stepName; // e.g. "Input Validation", "OCR Quality Gate"
        private String status; // PASSED, COMPLETED, BLOCKED, SKIPPED, WARNING
        private String detail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimContextInfo {
        @Builder.Default
        private List<String> geographicEntities = new ArrayList<>(); // e.g. ["Nepal", "India"]
        private String domain; // e.g. "Disaster Relief & Emergency Response", "Space Exploration", "Public Health"
        private String claimType; // e.g. "GOVERNMENT_ACTION", "DISASTER_CASUALTY", "SCIENTIFIC_DISCOVERY"
        @Builder.Default
        private List<String> targetAuthorityInstitutions = new ArrayList<>(); // e.g. ["Nepal Police", "National Disaster Management", "Ministry of External Affairs"]
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetrievalAudit {
        private int searchQueriesRun;
        private int sourcesRetrieved;
        private int relevantSources;
        private int rejectedSources;
        private int syndicatedDuplicates;
        private int independentClustersCount;
        private int primarySourcesCount;
        private int accreditedSecondaryCount;
        private int referenceSourcesCount;
        private int supportingSourcesCount;
        private int contradictingSourcesCount;
        private String auditSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EntityRelationshipTriple {
        private String subject; // e.g. "Nepal flash floods"
        private String predicate; // e.g. "caused", "resulted in", "sent"
        private String objectValue; // e.g. "95 deaths", "relief materials"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DecomposedClaim {
        private String claimText;
        private String claimType; // EVENT_OCCURRENCE, CASUALTY_COUNT, MISSING_COUNT, GOVERNMENT_ACTION, TEMPORAL_DATE, LOCATION_FACT, ATTRIBUTION, STATISTICAL_CLAIM
        private String claimCentrality; // PRIMARY_CLAIM, SUPPORTING_CLAIM, MINOR_CLAIM
        private double claimImportanceWeight; // e.g. 0.60 for Primary, 0.30 for Supporting, 0.10 for Minor
        private Integer claimScore; // 0 to 100
        private String claimVerdict; // VERIFIED, MOSTLY_VERIFIED, DEVELOPING_RANGE, REFUTED, UNVERIFIED, INSUFFICIENT_EVIDENCE
        private String stance; // SUPPORTED, CONFIRMED, ARTICLE_REPORTS_CLAIM, DEVELOPING, REFUTED, DENIED, PARTIALLY_SUPPORTED, NOT_MENTIONED, UNCERTAIN
        private String evidenceSummary;
        private String targetMetric; // e.g. "CASUALTY_COUNT = 95", "MISSING_COUNT >= 350"
        private String developingRange; // e.g. "95 - 102 reported casualties"
        private String statusReason;
        private EntityRelationshipTriple entityRelationship;
        @Builder.Default
        private List<String> evidenceClusterIds = new ArrayList<>();
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
        private boolean isPrimaryAuthority; // True if official emergency/police/gov authority
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExplainabilityProfile {
        private String confidenceLevel; // HIGH, MEDIUM, LOW
        private Integer confidenceScore; // 0 to 100%
        private Integer evidenceCompleteness; // 0 to 100%
        private String asOfStatus; // SUPPORTED_AT_CLAIM_TIME, CURRENTLY_VALID, OUTDATED_SUPERSEDED
        private String distortionType; // NUMERICAL_DISTORTION, LOCATION_DISTORTION, etc.
        @Builder.Default
        private List<String> positiveChecklist = new ArrayList<>(); // e.g. "✓ Corroborated across 2 independent wire clusters"
        @Builder.Default
        private List<String> warningChecklist = new ArrayList<>(); // e.g. "⚠ Numerical discrepancy: claim states 0, wire confirms 166"
        @Builder.Default
        private List<String> detectedDifferences = new ArrayList<>();
        @Builder.Default
        private List<EvidenceItemSummary> evidenceMatrix = new ArrayList<>();
        private RetrievalAudit retrievalAudit;
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
        private double contextualAuthorityScore; // 0.0 to 1.0
        private String geographicRelevance; // VERY_HIGH, HIGH, MEDIUM, LOW
        private String directness; // DIRECT_PRIMARY, SECONDARY_REPORTING, INDIRECT_REFERENCE
        private String evidenceRole; // "Direct Verification Evidence" vs "Discovery Candidate"
        @Builder.Default
        private List<String> acceptanceReasons = new ArrayList<>();
        @Builder.Default
        private List<String> rejectionFlags = new ArrayList<>();
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
        private String originalPublisher; // Fallback
        private String earliestIdentifiedPublisher; // Display
        private String earliestVerifiedSourceFound; // e.g. "Nepal Police", "The Hindu", "Reuters", "Snopes"
        private String originalDomain; // e.g. "thehindu.com"
        private String originalHeadline; // Exact original article title
        private String originalUrl; // Direct link
        private String publishedDate; // Publication date if available
        private String retrievalTimestamp; // Exact retrieval time
        private String provenanceType; // AUTHENTIC_REPRODUCTION, ALTERED_DISTORTION, DOCUMENTED_HOAX, UNVERIFIED_ORIGIN
        private String provenanceStatus; // PRIMARY_SOURCE_FOUND, EARLIEST_VERIFIED_SOURCE_FOUND, SECONDARY_SOURCE_FOUND, MULTIPLE_RELATED_SOURCES_FOUND, ORIGIN_UNDETERMINED
        private String claimIntegrity; // AUTHENTIC_REPRODUCTION, MINOR_VARIANCE, ALTERED_DISTORTION, FABRICATED_ASSERTION, DOCUMENTED_HOAX
        private String contradictionSeverity; // NONE, MINOR_DISCREPANCY, MODERATE_CONTRADICTION, MAJOR_CONTRADICTION, DIRECT_FACTUAL_REVERSAL
        private String evidenceTier; // LEVEL_1_PRIMARY, LEVEL_2_SECONDARY, LEVEL_3_FACTCHECK, LEVEL_4_REFERENCE
        private String distortionAnalysis; // Details of what was altered/manipulated
        private String crossReferencedConsensus; // e.g. "Corroborated across 3 Tier-1 wire agencies"
        private String provenanceConfidence; // HIGH, MEDIUM, LOW
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
        private double contextualAuthorityScore; // 0.0 to 1.0
        private String geographicRelevance; // VERY_HIGH, HIGH, MEDIUM, LOW
        private String directness; // DIRECT_PRIMARY, SECONDARY_REPORTING, INDIRECT_REFERENCE
        private String stance; // SUPPORTED, CONFIRMED, ARTICLE_REPORTS_CLAIM, REFUTED, DENIED, PARTIALLY_SUPPORTED, NOT_MENTIONED, UNCERTAIN
        private String verdictBySource; // True, False, Unverified, Reported Allegation
        private String articleTitle;
        private String url;
        private String clusterId;
        private boolean isPrimarySource;
        @Builder.Default
        private List<String> acceptanceReasons = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageIntegrityAnalysis {
        private String imageContentType; // NEWS_SCREENSHOT, SOCIAL_MEDIA_SCREENSHOT, NEWSPAPER_CLIPPING, NEWS_BANNER, ARTICLE_SCREENSHOT, INFOGRAPHIC, MEME, PHOTOGRAPH, ILLUSTRATION, DOCUMENT, UNKNOWN
        private String textPresence; // TEXT_PRESENT, TEXT_ABSENT, TEXT_UNCERTAIN
        private String rawOcrText; // Exact raw OCR string
        private String normalizedOcrText; // Whitespace & punctuation normalized
        private String reconstructedClaim; // Entity-resolved claim proposition
        private String detectedHeadlineText; // Backwards compatible alias for reconstructedClaim or normalized text
        private String claimVerificationBasis; // RAW_OCR, NORMALIZED_OCR, RECONSTRUCTED_CLAIM, USER_CORRECTED_OCR
        
        private Double ocrConfidence; // 0.0 to 100.0%
        private String ocrQualityLevel; // HIGH, MEDIUM, LOW, UNRELIABLE
        private String ocrConsistency; // HIGH, MEDIUM, LOW, N/A
        @Builder.Default
        private Integer ocrMultiPassCount = 3;
        private Double reconstructionConfidence; // 0.0 to 100.0%
        private Double garbageCharacterRatio; // 0.0 to 100.0%
        private Double validWordRatio; // 0.0 to 100.0%
        private Double entityConfidence; // 0.0 to 100.0%
        
        private String claimExtractionStatus; // CLAIM_READY_FOR_VERIFICATION, NO_TEXT_DETECTED, OCR_INSUFFICIENT, CLAIM_EXTRACTION_FAILED, NON_VERIFIABLE_CLAIM
        private boolean requiresUserReview;

        // Decoupled Image Forensics
        private double manipulationProbability; // Backwards compatible anomaly percentage (0% to 100%)
        private String forensicAssessment; // NO_SIGNIFICANT_ANOMALY, MINOR_ANOMALIES, ANOMALIES_DETECTED, STRONG_FORENSIC_CONCERNS, INCONCLUSIVE
        private String manipulationVerdict; // Human readable summary of forensic assessment
        private String imageContextStatus; // "Context Matches Claim", "Misleading / Repurposed Visual Context Likely", "Unverified Context"
        private String exifStatus; // "Sensor Metadata Available", "Stripped by Platform (Neutral)", "Edited Metadata"
        private String compressionAssessment; // "NORMAL", "ANOMALIES_DETECTED"
        private String pixelAnomalyAssessment; // "NOT_DETECTED", "POSSIBLE_ANOMALIES"
        private String forensicDisclaimer; // "Forensic indicators do not independently establish that an image has been manipulated."
        @Builder.Default
        private List<String> anomalyFlags = new ArrayList<>();
        private String heatmapOverlayUrl;
    }
}
