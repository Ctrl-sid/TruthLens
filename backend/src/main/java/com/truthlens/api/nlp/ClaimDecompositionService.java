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

    private static final Pattern NUMERICAL_PATTERN = Pattern.compile("(?i)\\b(?:killed|died|injured|fatalities|casualties|dead|toll|worth|cost|received|acquired|spent|recorded|amounted)\\s+(?:to\\s+)?(?:[0-9]+|none|zero|nil|no\\s+one|several|dozens|hundreds|thousands|millions|billions)\\b(?:\\s+(?:people|civilians|soldiers|officers|dollars|rupees|crore))?");
    private static final Pattern TEMPORAL_PATTERN = Pattern.compile("(?i)\\b(?:happened|occurred|took\\s+place|in|on|during|since|at|around)\\s+(?:the\\s+year\\s+)?(?:19|20)\\d{2}|\\b(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2}(?:,\\s*(?:19|20)\\d{2})?\\b");
    private static final Pattern NEGATION_PATTERN = Pattern.compile("(?i)\\b(not|did\\s+not|never|no|none|nobody|zero|denied|denies|rejected|rejects|dismissed|failed\\s+to|without)\\b");

    public List<DecomposedClaim> decompose(String text) {
        List<DecomposedClaim> subClaims = new ArrayList<>();
        if (text == null || text.isBlank()) return subClaims;

        String cleaned = text.trim();

        // 1. Check for explicit conjunction splitting (" and ", " but ", " while ", " whereas ", "; ")
        String[] segments = cleaned.split("(?i)\\s+(?:and|but|whereas|while|yet|although)\\s+|;\\s+");

        if (segments.length > 1) {
            String subjectContext = extractSubjectContext(segments[0]);
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i].trim();
                if (segment.length() < 4) continue;

                // Prepend subject context if subsequent clause starts with a verb/predicate
                String fullProposition = (i > 0 && !hasNounSubject(segment) && !subjectContext.isBlank()) ?
                        subjectContext + " " + segment : segment;

                String type = classifyClaimType(fullProposition);
                String centrality = determineCentrality(type, i);
                double weight = determineCentralityWeight(centrality);
                boolean isNegated = NEGATION_PATTERN.matcher(fullProposition).find();

                subClaims.add(DecomposedClaim.builder()
                        .claimText(capitalizeFirst(fullProposition))
                        .claimType(type)
                        .claimCentrality(centrality)
                        .claimImportanceWeight(weight)
                        .claimScore(75) // Base pending verification
                        .claimVerdict("PENDING")
                        .stance("UNCERTAIN")
                        .evidenceSummary("Pending independent sub-claim cross-referencing.")
                        .isNegated(isNegated)
                        .build());
            }
        }

        // 2. If no conjunction split was found, decompose into entity-fact dimensions if multiple predicates exist
        if (subClaims.isEmpty()) {
            Matcher numMatcher = NUMERICAL_PATTERN.matcher(cleaned);
            Matcher tempMatcher = TEMPORAL_PATTERN.matcher(cleaned);

            if (numMatcher.find() && tempMatcher.find()) {
                String subjectContext = extractSubjectContext(cleaned);
                String numFact = numMatcher.group(0);
                String tempFact = tempMatcher.group(0);

                String text1 = capitalizeFirst(subjectContext + " " + numFact);
                String text2 = capitalizeFirst(subjectContext + " " + tempFact);

                subClaims.add(DecomposedClaim.builder()
                        .claimText(text1)
                        .claimType("CASUALTY_COUNT")
                        .claimCentrality("PRIMARY_CLAIM")
                        .claimImportanceWeight(0.60)
                        .claimScore(75)
                        .claimVerdict("PENDING")
                        .stance("UNCERTAIN")
                        .evidenceSummary("Numerical/Casualty sub-claim component.")
                        .isNegated(NEGATION_PATTERN.matcher(text1).find())
                        .build());

                subClaims.add(DecomposedClaim.builder()
                        .claimText(text2)
                        .claimType("TEMPORAL_DATE")
                        .claimCentrality("SUPPORTING_CLAIM")
                        .claimImportanceWeight(0.40)
                        .claimScore(75)
                        .claimVerdict("PENDING")
                        .stance("UNCERTAIN")
                        .evidenceSummary("Temporal/Date sub-claim component.")
                        .isNegated(NEGATION_PATTERN.matcher(text2).find())
                        .build());
            } else {
                // Single atomic claim
                String type = classifyClaimType(cleaned);
                String centrality = "PRIMARY_CLAIM";
                boolean isNegated = NEGATION_PATTERN.matcher(cleaned).find();

                subClaims.add(DecomposedClaim.builder()
                        .claimText(capitalizeFirst(cleaned))
                        .claimType(type)
                        .claimCentrality(centrality)
                        .claimImportanceWeight(1.00)
                        .claimScore(75)
                        .claimVerdict("PENDING")
                        .stance("UNCERTAIN")
                        .evidenceSummary("Atomic factual statement.")
                        .isNegated(isNegated)
                        .build());
            }
        }

        return subClaims;
    }

    private String determineCentrality(String claimType, int index) {
        if ("CASUALTY_COUNT".equals(claimType) || "EVENT_OCCURRENCE".equals(claimType) || "STATISTICAL_CLAIM".equals(claimType) || index == 0) {
            return "PRIMARY_CLAIM";
        }
        if ("LOCATION_FACT".equals(claimType) || "TEMPORAL_DATE".equals(claimType)) {
            return "SUPPORTING_CLAIM";
        }
        return "MINOR_CLAIM";
    }

    private double determineCentralityWeight(String centrality) {
        switch (centrality) {
            case "PRIMARY_CLAIM": return 0.60;
            case "SUPPORTING_CLAIM": return 0.30;
            default: return 0.10;
        }
    }

    private String classifyClaimType(String proposition) {
        String lower = proposition.toLowerCase();
        if (lower.contains("killed") || lower.contains("died") || lower.contains("casualties") || lower.contains("none") || lower.contains("zero") || lower.contains("cost") || lower.contains("billion") || lower.contains("million")) {
            return "CASUALTY_COUNT";
        }
        if (lower.matches(".*\\b(19|20)\\d{2}\\b.*") || lower.contains("yesterday") || lower.contains("today") || lower.contains("tomorrow") || lower.contains("monday") || lower.contains("sunday")) {
            return "TEMPORAL_DATE";
        }
        if (lower.contains(" in ") || lower.contains(" at ") || lower.contains("location") || lower.contains("near")) {
            return "LOCATION_FACT";
        }
        if (lower.contains("said") || lower.contains("announced") || lower.contains("claimed") || lower.contains("stated") || lower.contains("denied")) {
            return "ATTRIBUTION";
        }
        if (lower.contains("percent") || lower.contains("%") || lower.contains("rate") || lower.contains("increase") || lower.contains("decrease")) {
            return "STATISTICAL_CLAIM";
        }
        return "EVENT_OCCURRENCE";
    }

    private String extractSubjectContext(String text) {
        if (text == null) return "";
        String[] parts = text.split("(?i)\\b(killed|died|happened|occurred|took\\s+place|stated|claimed|announced|was|were|has|have|is|are)\\b", 2);
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
        return !first.matches("^(and|or|happened|occurred|killed|died|took|was|were|has|have|had|is|are|in|on|at|with|by)$");
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isBlank()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
