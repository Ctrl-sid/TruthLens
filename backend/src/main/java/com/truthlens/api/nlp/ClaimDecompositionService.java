package com.truthlens.api.nlp;

import com.truthlens.api.dto.ClaimVerificationResponse.DecomposedClaim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimDecompositionService {

    private static final Pattern CASUALTY_PATTERN = Pattern.compile("(?i)\\b(?:at\\s+least\\s+)?(?:killed|died|dead|fatalities|casualties|toll)\\s+(?:reaches\\s+|to\\s+)?(?:[0-9]+|none|zero|nil|no\\s+one|several|dozens|hundreds)\\b|\\b(?:at\\s+least\\s+)?(?:[0-9]+|none|zero)\\s+(?:dead|killed|fatalities|deaths)\\b");
    private static final Pattern MISSING_PATTERN = Pattern.compile("(?i)\\b(?:over|more\\s+than|at\\s+least|about)?\\s*(\\d+)\\+?\\s+(?:are\\s+)?missing\\b");
    private static final Pattern RELIEF_PATTERN = Pattern.compile("(?i)\\b(?:sends?|sent|dispatched|provided|offered)\\s+(?:first|second|immediate)?\\s*(?:tranche\\s+of\\s+)?(?:relief|humanitarian|medical|aid|supplies|materials|assistance)\\b");
    private static final Pattern TEMPORAL_PATTERN = Pattern.compile("(?i)\\b(?:happened|occurred|took\\s+place|in|on|during|since|at|around)\\s+(?:the\\s+year\\s+)?(?:19|20)\\d{2}|\\b(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2}(?:,\\s*(?:19|20)\\d{2})?\\b");
    private static final Pattern NEGATION_PATTERN = Pattern.compile("(?i)\\b(not|did\\s+not|never|no|none|nobody|zero|denied|denies|rejected|rejects|dismissed|failed\\s+to|without)\\b");

    public List<DecomposedClaim> decompose(String text) {
        List<DecomposedClaim> subClaims = new ArrayList<>();
        if (text == null || text.isBlank()) return subClaims;

        String cleaned = text.trim()
                .replaceAll("(?i)\\b(?:LIVE|BREAKING|EXCLUSIVE|UPDATE):?\\s*", "")
                .trim();

        // 1. Check for compound semi-colon, comma, or conjunction clause splitting
        String[] clauses = cleaned.split("(?i);\\s*|\\s+(?:and|but|while|whereas)\\s+|(?<=\\d)\\s*,\\s*(?=[a-zA-Z])");

        if (clauses.length > 1) {
            String primarySubject = extractSubjectContext(clauses[0]);

            for (int i = 0; i < clauses.length; i++) {
                String clause = clauses[i].trim();
                if (clause.length() < 4) continue;

                String proposition = clause;
                if (i > 0 && !hasNounSubject(clause) && !primarySubject.isBlank()) {
                    proposition = primarySubject + " " + clause;
                }

                String type = classifyClaimType(proposition);
                String centrality = determineCentrality(type, i);
                double weight = determineCentralityWeight(centrality, clauses.length);
                String metric = extractTargetMetric(proposition, type);
                boolean isNegated = NEGATION_PATTERN.matcher(proposition).find();

                subClaims.add(DecomposedClaim.builder()
                        .claimText(capitalizeFirst(proposition))
                        .claimType(type)
                        .claimCentrality(centrality)
                        .claimImportanceWeight(weight)
                        .targetMetric(metric)
                        .claimScore(75) // Pending verification
                        .claimVerdict("PENDING")
                        .stance("UNCERTAIN")
                        .evidenceSummary("Pending contextual cross-referencing.")
                        .isNegated(isNegated)
                        .build());
            }
        }

        // 2. If single sentence with multiple extracted predicates (e.g. casualties + missing + action)
        if (subClaims.isEmpty() || subClaims.size() == 1) {
            List<DecomposedClaim> predicateClaims = extractPredicateComponents(cleaned);
            if (predicateClaims.size() > 1) {
                return predicateClaims;
            }
        }

        // 3. Fallback: Atomic statement
        if (subClaims.isEmpty()) {
            String type = classifyClaimType(cleaned);
            String metric = extractTargetMetric(cleaned, type);
            boolean isNegated = NEGATION_PATTERN.matcher(cleaned).find();

            subClaims.add(DecomposedClaim.builder()
                    .claimText(capitalizeFirst(cleaned))
                    .claimType(type)
                    .claimCentrality("PRIMARY_CLAIM")
                    .claimImportanceWeight(1.00)
                    .targetMetric(metric)
                    .claimScore(75)
                    .claimVerdict("PENDING")
                    .stance("UNCERTAIN")
                    .evidenceSummary("Atomic factual proposition.")
                    .isNegated(isNegated)
                    .build());
        }

        return subClaims;
    }

    private List<DecomposedClaim> extractPredicateComponents(String text) {
        List<DecomposedClaim> list = new ArrayList<>();
        String subject = extractSubjectContext(text);

        Matcher casMatcher = CASUALTY_PATTERN.matcher(text);
        Matcher misMatcher = MISSING_PATTERN.matcher(text);
        Matcher relMatcher = RELIEF_PATTERN.matcher(text);

        if (casMatcher.find()) {
            String casText = casMatcher.group(0);
            list.add(DecomposedClaim.builder()
                    .claimText(capitalizeFirst(subject + " casualty toll: " + casText))
                    .claimType("CASUALTY_COUNT")
                    .claimCentrality("PRIMARY_CLAIM")
                    .claimImportanceWeight(0.40)
                    .targetMetric(extractTargetMetric(casText, "CASUALTY_COUNT"))
                    .claimScore(75)
                    .claimVerdict("PENDING")
                    .stance("UNCERTAIN")
                    .evidenceSummary("Casualty sub-claim component.")
                    .isNegated(NEGATION_PATTERN.matcher(casText).find())
                    .build());
        }

        if (misMatcher.find()) {
            String misText = misMatcher.group(0);
            list.add(DecomposedClaim.builder()
                    .claimText(capitalizeFirst(subject + " missing persons: " + misText))
                    .claimType("MISSING_COUNT")
                    .claimCentrality("SUPPORTING_CLAIM")
                    .claimImportanceWeight(0.30)
                    .targetMetric(extractTargetMetric(misText, "MISSING_COUNT"))
                    .claimScore(75)
                    .claimVerdict("PENDING")
                    .stance("UNCERTAIN")
                    .evidenceSummary("Missing person sub-claim component.")
                    .isNegated(false)
                    .build());
        }

        if (relMatcher.find()) {
            String relText = relMatcher.group(0);
            list.add(DecomposedClaim.builder()
                    .claimText(capitalizeFirst("Relief operations: " + relText))
                    .claimType("GOVERNMENT_ACTION")
                    .claimCentrality("SUPPORTING_CLAIM")
                    .claimImportanceWeight(0.30)
                    .targetMetric("RELIEF_DISPATCH = TRUE")
                    .claimScore(75)
                    .claimVerdict("PENDING")
                    .stance("UNCERTAIN")
                    .evidenceSummary("Relief and assistance sub-claim component.")
                    .isNegated(false)
                    .build());
        }

        return list;
    }

    private String extractTargetMetric(String text, String claimType) {
        String lower = text.toLowerCase();
        if ("CASUALTY_COUNT".equals(claimType)) {
            Matcher m = Pattern.compile("(\\b(?:\\d+|none|zero)\\b)").matcher(lower);
            if (m.find()) {
                return "CASUALTY_COUNT = " + m.group(1);
            }
        }
        if ("MISSING_COUNT".equals(claimType)) {
            Matcher m = Pattern.compile("(\\b\\d+\\b)").matcher(lower);
            if (m.find()) {
                return "MISSING_COUNT >= " + m.group(1);
            }
        }
        if ("GOVERNMENT_ACTION".equals(claimType)) {
            return "GOVERNMENT_ACTION_DISPATCHED";
        }
        return "QUALITATIVE_ASSERTION";
    }

    private String determineCentrality(String claimType, int index) {
        if ("CASUALTY_COUNT".equals(claimType) || "EVENT_OCCURRENCE".equals(claimType) || index == 0) {
            return "PRIMARY_CLAIM";
        }
        if ("MISSING_COUNT".equals(claimType) || "GOVERNMENT_ACTION".equals(claimType) || "LOCATION_FACT".equals(claimType) || "TEMPORAL_DATE".equals(claimType)) {
            return "SUPPORTING_CLAIM";
        }
        return "MINOR_CLAIM";
    }

    private double determineCentralityWeight(String centrality, int totalClauses) {
        if (totalClauses >= 4) {
            return "PRIMARY_CLAIM".equals(centrality) ? 0.35 : 0.22;
        }
        switch (centrality) {
            case "PRIMARY_CLAIM": return 0.60;
            case "SUPPORTING_CLAIM": return 0.30;
            default: return 0.10;
        }
    }

    private String classifyClaimType(String proposition) {
        String lower = proposition.toLowerCase();
        if (lower.contains("missing") || lower.contains("unaccounted")) {
            return "MISSING_COUNT";
        }
        if (lower.contains("sends") || lower.contains("sent") || lower.contains("relief") || lower.contains("tranche") || lower.contains("assistance") || lower.contains("approved")) {
            return "GOVERNMENT_ACTION";
        }
        if (lower.contains("killed") || lower.contains("died") || lower.contains("dead") || lower.contains("casualties") || lower.contains("fatalities") || lower.contains("toll")) {
            return "CASUALTY_COUNT";
        }
        if (lower.matches(".*\\b(19|20)\\d{2}\\b.*") || lower.contains("yesterday") || lower.contains("today") || lower.contains("tomorrow")) {
            return "TEMPORAL_DATE";
        }
        if (lower.contains(" in ") || lower.contains(" at ") || lower.contains("location") || lower.contains("near")) {
            return "LOCATION_FACT";
        }
        if (lower.contains("said") || lower.contains("announced") || lower.contains("claimed") || lower.contains("stated") || lower.contains("denied")) {
            return "ATTRIBUTION";
        }
        return "EVENT_OCCURRENCE";
    }

    private String extractSubjectContext(String text) {
        if (text == null) return "";
        String[] parts = text.split("(?i)\\b(killed|died|dead|happened|occurred|sends|sent|took\\s+place|stated|claimed|announced|was|were|has|have|is|are)\\b", 2);
        if (parts.length > 0 && !parts[0].isBlank()) {
            return parts[0].trim();
        }
        String[] words = text.split("\\s+");
        if (words.length >= 3) {
            return String.join(" ", words[0], words[1], words[2]);
        }
        return text.trim();
    }

    private boolean hasNounSubject(String segment) {
        String[] words = segment.trim().split("\\s+");
        if (words.length == 0) return false;
        String first = words[0].toLowerCase();
        return !first.matches("^(and|or|happened|occurred|killed|died|dead|took|was|were|has|have|had|is|are|in|on|at|with|by|over|at\\s+least)$");
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isBlank()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
