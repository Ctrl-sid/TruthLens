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
    private String timestamp;

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
