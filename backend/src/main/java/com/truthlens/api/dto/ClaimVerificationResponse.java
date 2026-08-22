package com.truthlens.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String verdict; // VERIFIED GENUINE, MOSTLY GENUINE, MIXED / UNVERIFIED, LIKELY FAKE, FABRICATED
    private String verdictBadgeColor; // #10B981, #F59E0B, #EF4444
    private String rationale;
    private List<String> keyReasons;
    private List<SourceEvidence> sources;
    private NlpAnalysisResponse nlpAnalysis;
    private ImageIntegrityAnalysis imageAnalysis; // Null if not image input
    private ClaimOriginDiscovery originDiscovery;
    private String timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimOriginDiscovery {
        private String originalPublisher; // e.g. "Deccan Herald", "The Hindu", "Snopes"
        private String originalDomain; // e.g. "deccanherald.com"
        private String originalHeadline; // Exact original article title
        private String originalUrl; // Direct link
        private String publishedDate; // Publication date if available
        private String provenanceType; // AUTHENTIC_REPRODUCTION, ALTERED_DISTORTION, DOCUMENTED_HOAX, UNVERIFIED_ORIGIN
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
        private int credibilityRating;
        private double matchPercentage;
        private String verdictBySource; // True, False, Unverified
        private String articleTitle;
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageIntegrityAnalysis {
        private String detectedHeadlineText;
        private double manipulationProbability; // 0% to 100%
        private String exifStatus; // "Clean EXIF", "Stripped / Edited Metadata"
        private List<String> anomalyFlags; // ["Noise Variance Artifacts", "Text Overlay Inconsistency"]
        private String heatmapOverlayUrl;
    }
}
