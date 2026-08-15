package com.truthlens.api.nlp;

import com.truthlens.api.dto.NlpAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NlpPipelineService {

    private final NamedEntityExtractor entityExtractor;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final ClickbaitClassifier clickbaitClassifier;

    public NlpAnalysisResponse processText(String text) {
        Map<String, String> entityMap = entityExtractor.extractEntities(text);
        List<String> entities = new ArrayList<>(entityMap.keySet());

        double sentiment = sentimentAnalyzer.calculateSentimentScore(text);
        double subjectivity = sentimentAnalyzer.calculateSubjectivityScore(text);

        List<String> flags = new ArrayList<>();
        double clickbaitScore = clickbaitClassifier.calculateClickbaitScore(text, flags);

        String tone;
        if (clickbaitScore > 60) {
            tone = "High Sensationalism & Hyperbolic Clickbait";
        } else if (subjectivity > 0.6) {
            tone = "Subjective & Opinionated";
        } else if (sentiment > 0.3) {
            tone = "Positive & Informative";
        } else if (sentiment < -0.3) {
            tone = "Critical / Negative Focus";
        } else {
            tone = "Objective & Neutral";
        }

        // Flesch-Kincaid estimate
        int wordCount = text.split("\\s+").length;
        int readabilityScore = Math.max(30, Math.min(95, 100 - (wordCount / 3)));

        return NlpAnalysisResponse.builder()
                .extractedEntities(entities)
                .entityCategories(entityMap)
                .sentimentScore(Math.round(sentiment * 100.0) / 100.0)
                .subjectivityScore(Math.round(subjectivity * 100.0) / 100.0)
                .clickbaitRating(clickbaitScore)
                .toneAnalysis(tone)
                .readabilityScore(readabilityScore)
                .exaggerationFlags(flags)
                .build();
    }
}
