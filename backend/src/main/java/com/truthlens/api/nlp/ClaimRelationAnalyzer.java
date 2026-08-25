package com.truthlens.api.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClaimRelationAnalyzer {

    public enum StanceRelation {
        SUPPORTED,
        CONFIRMED,
        DENIED,
        REFUTED,
        CONTRADICTED,
        UNCERTAIN,
        NOT_MENTIONED
    }

    public StanceRelation analyzeRelation(String claim, String evidenceHeadline, boolean isContradicted) {
        if (claim == null || evidenceHeadline == null) return StanceRelation.UNCERTAIN;
        if (isContradicted) return StanceRelation.REFUTED;

        String cLower = claim.toLowerCase().trim();
        String eLower = evidenceHeadline.toLowerCase().trim();

        // Check for explicit denial verbs in headline
        if (eLower.contains("denies") || eLower.contains("denied") || eLower.contains("rejects") || eLower.contains("dismisses") || eLower.contains("refutes")) {
            return StanceRelation.DENIED;
        }

        // Check for official confirmation verbs
        if (eLower.contains("confirms") || eLower.contains("confirmed") || eLower.contains("affirms") || eLower.contains("verifies") || eLower.contains("announces")) {
            return StanceRelation.CONFIRMED;
        }

        // Check for fact-check debunking
        if (eLower.contains("debunk") || eLower.contains("fact check: no") || eLower.contains("false:") || eLower.contains("hoax") || eLower.contains("fake news")) {
            return StanceRelation.REFUTED;
        }

        // Positive corroboration
        return StanceRelation.SUPPORTED;
    }
}
