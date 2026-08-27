package com.truthlens.api.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClaimRelationAnalyzer {

    public enum StanceRelation {
        SUPPORTED,
        CONFIRMED,
        ARTICLE_REPORTS_CLAIM,
        REFUTED,
        DENIED,
        PARTIALLY_SUPPORTED,
        NOT_MENTIONED,
        UNCERTAIN
    }

    public StanceRelation analyzeRelation(String claim, String evidenceHeadline, boolean isContradicted) {
        if (claim == null || evidenceHeadline == null) return StanceRelation.UNCERTAIN;
        if (isContradicted) return StanceRelation.REFUTED;

        String cLower = claim.toLowerCase().trim();
        String eLower = evidenceHeadline.toLowerCase().trim();

        // Check for official denial
        if (eLower.contains("denies") || eLower.contains("denied") || eLower.contains("rejects") || eLower.contains("dismisses") || eLower.contains("refutes")) {
            return StanceRelation.DENIED;
        }

        // Check for fact-check debunking
        if (eLower.contains("debunk") || eLower.contains("fact check: no") || eLower.contains("false:") || eLower.contains("hoax") || eLower.contains("fake news")) {
            return StanceRelation.REFUTED;
        }

        // Check for reporting an unverified allegation / claim
        if (eLower.contains("claims") || eLower.contains("alleges") || eLower.contains("alleged") || eLower.contains("purported") || eLower.contains("according to")) {
            return StanceRelation.ARTICLE_REPORTS_CLAIM;
        }

        // Check for official confirmation
        if (eLower.contains("confirms") || eLower.contains("confirmed") || eLower.contains("affirms") || eLower.contains("verifies") || eLower.contains("official statement")) {
            return StanceRelation.CONFIRMED;
        }

        // Check if evidence headline is completely unrelated to specific predicate
        if (!hasSignificantOverlap(cLower, eLower)) {
            return StanceRelation.NOT_MENTIONED;
        }

        return StanceRelation.SUPPORTED;
    }

    private boolean hasSignificantOverlap(String claim, String evidence) {
        String[] words = claim.split("[^a-zA-Z0-9]+");
        int matchCount = 0;
        int totalSignificant = 0;

        for (String w : words) {
            if (w.length() > 3 && !isStopWord(w)) {
                totalSignificant++;
                if (evidence.contains(w)) {
                    matchCount++;
                }
            }
        }

        return totalSignificant == 0 || ((double) matchCount / totalSignificant) >= 0.25;
    }

    private boolean isStopWord(String word) {
        return word.matches("^(the|and|for|with|from|this|that|have|been|were|they|what|when|where|which|about)$");
    }
}
