package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimVerificationResponse.ImageIntegrityAnalysis;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OcrAnalysisService {

    public ImageIntegrityAnalysis analyzeImageInput(String imageContent) {
        List<String> anomalyFlags = new ArrayList<>();

        // Simulated Deep Image Inspection & OCR Text Extraction
        String extractedHeadline;
        double manipulationProbability;

        if (imageContent != null && imageContent.toLowerCase().contains("cure")) {
            extractedHeadline = "SECRET MIRACLE CURE REVEALED BY ANONYMOUS SCIENTISTS!";
            manipulationProbability = 82.5;
            anomalyFlags.add("Font Alignment Distortion Detected in Header");
            anomalyFlags.add("Compression Artifact Variance around Text Region");
            anomalyFlags.add("Missing Camera EXIF Metadata");
        } else if (imageContent != null && imageContent.toLowerCase().contains("deepfake")) {
            extractedHeadline = "Leaked Audio & Photo of Banking CEO Discussing Forgiveness";
            manipulationProbability = 91.0;
            anomalyFlags.add("Facial Boundary Frequency Discrepancy");
            anomalyFlags.add("Generative AI Pixel Noise Correlation");
        } else {
            extractedHeadline = "NASA James Webb Space Telescope Discovers Atmospheric Water Vapor on Exoplanet";
            manipulationProbability = 4.2;
            anomalyFlags.add("Clean EXIF Profile Validated");
            anomalyFlags.add("Consistent Sensor Noise Pattern Across RGB Channels");
        }

        return ImageIntegrityAnalysis.builder()
                .detectedHeadlineText(extractedHeadline)
                .manipulationProbability(manipulationProbability)
                .exifStatus(manipulationProbability > 50 ? "Stripped / Edited Metadata" : "Authentic Sensor Metadata")
                .anomalyFlags(anomalyFlags)
                .heatmapOverlayUrl("https://images.unsplash.com/photo-1507499739999-097706ad8914?w=600&auto=format&fit=crop")
                .build();
    }
}
