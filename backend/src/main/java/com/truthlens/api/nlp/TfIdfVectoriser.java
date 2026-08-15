package com.truthlens.api.nlp;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TfIdfVectoriser {

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

        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        int totalWords = words.length;

        for (String word : words) {
            if (word.length() > 2) {
                tfMap.put(word, tfMap.getOrDefault(word, 0.0) + 1.0);
            }
        }

        for (Map.Entry<String, Double> entry : tfMap.entrySet()) {
            tfMap.put(entry.getKey(), entry.getValue() / totalWords);
        }

        return tfMap;
    }
}
