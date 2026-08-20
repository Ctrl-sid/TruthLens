package com.truthlens.api.nlp;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SentimentAnalyzer {

    private static final Set<String> POSITIVE_LEXICON = Set.of(
            "VERIFIED", "CONFIRMED", "AUTHENTIC", "DISCOVERY", "ACCURATE", "OFFICIAL",
            "PROVEN", "BREAKTHROUGH", "GENUINE", "VALIDATED", "FACTUAL", "SUPPORTED"
    );

    private static final Set<String> NEGATIVE_LEXICON = Set.of(
            "DEEPFAKE", "HOAX", "FAKE", "SCAM", "FABRICATED", "MISLEADING", "SHOCKING",
            "DECEPTIVE", "CONSPIRACY", "UNVERIFIED", "FALSE", "DANGEROUS", "CLAIMED", "RUMOR"
    );

    private static final Set<String> SUBJECTIVE_TRIGGER_WORDS = Set.of(
            "SHOCKING", "UNBELIEVABLE", "SECRET", "MIRACLE", "REVEALED", "MUST SEE",
            "EXPOSED", "INSANE", "DISASTER", "TERRIFYING", "HORRIFYING", "AMAZING"
    );

    public double calculateSentimentScore(String text) {
        if (text == null || text.isBlank()) return 0.0;
        String[] tokens = text.toUpperCase().split("\\W+");
        int posCount = 0;
        int negCount = 0;

        for (String token : tokens) {
            if (POSITIVE_LEXICON.contains(token)) posCount++;
            if (NEGATIVE_LEXICON.contains(token)) negCount++;
        }

        int total = posCount + negCount;
        if (total == 0) return 0.0;
        return (double) (posCount - negCount) / total;
    }

    public double calculateSubjectivityScore(String text) {
        if (text == null || text.isBlank()) return 0.2;
        String[] tokens = text.toUpperCase().split("\\W+");
        int subjectiveCount = 0;

        for (String token : tokens) {
            if (SUBJECTIVE_TRIGGER_WORDS.contains(token)) subjectiveCount++;
        }

        double score = (double) subjectiveCount / Math.max(1.0, tokens.length / 10.0);
        return Math.min(1.0, Math.max(0.1, score));
    }
}
