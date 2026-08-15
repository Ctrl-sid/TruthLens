package com.truthlens.api.nlp;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CosineSimilarityEngine {

    public double computeCosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        if (vecA.isEmpty() || vecB.isEmpty()) return 0.0;

        Set<String> allKeys = new HashSet<>(vecA.keySet());
        allKeys.addAll(vecB.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String key : allKeys) {
            double valA = vecA.getOrDefault(key, 0.0);
            double valB = vecB.getOrDefault(key, 0.0);

            dotProduct += valA * valB;
            normA += valA * valA;
            normB += valB * valB;
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
