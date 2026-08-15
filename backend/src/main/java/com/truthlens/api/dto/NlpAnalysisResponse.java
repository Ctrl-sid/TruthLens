package com.truthlens.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NlpAnalysisResponse {
    private List<String> extractedEntities; // ["NASA", "James Webb", "Exoplanet"]
    private Map<String, String> entityCategories; // {"NASA": "ORGANIZATION", "Exoplanet": "ASTRONOMY"}
    private double sentimentScore; // -1.0 to +1.0
    private double subjectivityScore; // 0.0 (Objective) to 1.0 (Subjective)
    private double clickbaitRating; // 0% to 100%
    private String toneAnalysis; // e.g. "Informative & Objective", "Sensationalist & Hyperbolic"
    private int readabilityScore; // Flesch-Kincaid index
    private List<String> exaggerationFlags; // e.g. ["All-Caps Keywords", "Urgent Call to Action"]
}
