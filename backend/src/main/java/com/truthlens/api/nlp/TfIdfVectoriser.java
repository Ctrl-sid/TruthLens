package com.truthlens.api.nlp;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TfIdfVectoriser {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "from", "up", "about", "into", "over", "after", "is", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would",
            "shall", "should", "may", "might", "must", "can", "could", "that", "this", "these",
            "those", "it", "its", "as", "if", "than", "because", "while", "where", "when", "how",
            "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no",
            "nor", "not", "only", "own", "same", "so", "too", "very", "just"
    );

    public Map<String, Double> createTfIdfVector(String text, List<String> corpus) {
        Map<String, Double> tfVector = calculateTermFrequency(text);
        Map<String, Double> tfIdfVector = new HashMap<>();

        int totalDocs = corpus.size() + 1;
        for (Map.Entry<String, Double> entry : tfVector.entrySet()) {
            String term = entry.getKey();
            double tf = entry.getValue();

            int docCount = 1;
            for (String doc : corpus) {
                if (doc.toLowerCase().contains(term)) {
                    docCount++;
                }
            }

            double idf = Math.log((double) totalDocs / docCount) + 1.0;
            tfIdfVector.put(term, tf * idf);
        }

        return tfIdfVector;
    }

    public Map<String, Double> calculateTermFrequency(String text) {
        Map<String, Double> tfMap = new HashMap<>();
        if (text == null || text.isBlank()) return tfMap;

        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        int validWordCount = 0;

        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.length() > 2 && !STOP_WORDS.contains(trimmed)) {
                tfMap.put(trimmed, tfMap.getOrDefault(trimmed, 0.0) + 1.0);
                validWordCount++;
            }
        }

        if (validWordCount > 0) {
            for (Map.Entry<String, Double> entry : tfMap.entrySet()) {
                tfMap.put(entry.getKey(), entry.getValue() / validWordCount);
            }
        }

        return tfMap;
    }
}
