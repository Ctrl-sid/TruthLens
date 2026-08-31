package com.truthlens.api.nlp;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ClaimVerifiabilityValidator {

    private static final Set<String> COMMON_GREETINGS = Set.of(
            "hello", "hi", "hey", "good morning", "good evening", "good afternoon",
            "how are you", "who are you", "what are you", "tell me a joke", "thanks", "thank you",
            "test", "testing", "help", "ok", "okay", "bye", "goodbye"
    );

    private static final Pattern REPETITIVE_GIBBERISH_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9])\\1{4,}$|^[b-df-hj-np-tv-z]{6,}$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern QUESTION_EXTRACTOR_PATTERN = Pattern.compile(
            "(?i)^(?:did|does|do|is\\s+it\\s+true\\s+that|has|have|had|was|were|will|would|can|could|is|are)\\s+(.+?)\\??$"
    );

    @Getter
    @Builder
    public static class ValidationResult {
        private boolean isVerifiableClaim;
        private String extractedFactualClaim; // Declarative claim extracted from question if applicable
        private String rejectionReason;
        private List<String> advisoryNotes;
    }

    public ValidationResult validateClaimVerifiability(String text) {
        if (text == null || text.isBlank()) {
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Empty input payload")
                    .advisoryNotes(List.of("Please provide a news headline, statement, or article URL."))
                    .build();
        }

        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();

        // 0. URLs are directly verifiable via domain & wire lookups
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.matches("^(?i)(www\\.[a-zA-Z0-9-]+\\.[a-z]{2,}|[a-zA-Z0-9-]+\\.(com|org|net|edu|gov|io|in|co|uk|ai)/.*)")) {
            return ValidationResult.builder()
                    .isVerifiableClaim(true)
                    .extractedFactualClaim(trimmed)
                    .build();
        }

        String[] words = trimmed.split("\\s+");
        List<String> notes = new ArrayList<>();

        // 1. Check for casual greetings / chatbot conversational prompts
        if (COMMON_GREETINGS.contains(lower) || lower.matches("^(hi|hello|hey)\\b.*")) {
            notes.add("TruthLens is an automated verification engine for news assertions, not a conversational chatbot.");
            notes.add("Please enter a factual claim statement (e.g., 'NASA discovers atmospheric water vapor on exoplanet').");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Conversational greeting or non-factual prompt detected")
                    .advisoryNotes(notes)
                    .build();
        }

        // 2. Check for single-character or keyboard gibberish
        if (REPETITIVE_GIBBERISH_PATTERN.matcher(trimmed).find() || (words.length == 1 && trimmed.length() > 15 && !trimmed.contains("."))) {
            notes.add("The submitted input appears to be random or unstructured characters.");
            notes.add("Please provide a clear textual statement or headline.");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Unstructured gibberish or random character sequence")
                    .advisoryNotes(notes)
                    .build();
        }

        // 3. Check for single word inputs without predicate
        if (words.length == 1 && !trimmed.contains(".")) {
            notes.add("A single word without an event predicate cannot be verified as true or false.");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Single word input lacking factual predicate")
                    .advisoryNotes(notes)
                    .build();
        }

        // 4. Check for WHO organization vs Interrogative Questions
        boolean endsWithQuestion = trimmed.endsWith("?");
        boolean isWhoOrgHeadline = (trimmed.startsWith("WHO ") || trimmed.startsWith("W.H.O. ")) && !endsWithQuestion &&
                (lower.contains("approves") || lower.contains("declares") || lower.contains("publishes") || lower.contains("warns") ||
                 lower.contains("reports") || lower.contains("confirms") || lower.contains("recommends") || lower.contains("states") ||
                 lower.contains("guidelines") || lower.contains("vaccine") || lower.contains("health") || lower.contains("officially"));

        if (isWhoOrgHeadline) {
            return ValidationResult.builder()
                    .isVerifiableClaim(true)
                    .extractedFactualClaim(trimmed)
                    .build();
        }

        // 5. Question Handling: Extract embedded factual proposition
        boolean isOpenEndedQuestion = lower.startsWith("what is ") || lower.startsWith("what are ") ||
                                      lower.startsWith("who is ") || lower.startsWith("who was ") ||
                                      lower.startsWith("why did ") || lower.startsWith("how to ") ||
                                      lower.startsWith("where is ");

        if (isOpenEndedQuestion) {
            notes.add("Open-ended inquiries ('What is...', 'Who is...') cannot be verified because they lack a declarative factual assertion.");
            notes.add("Please formulate as a declarative statement (e.g., 'James Webb telescope discovers water on LHS 1140b').");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Open-ended interrogative inquiry without declarative claim")
                    .advisoryNotes(notes)
                    .build();
        }

        Matcher qMatcher = QUESTION_EXTRACTOR_PATTERN.matcher(trimmed);
        if (qMatcher.find() && words.length >= 4) {
            String extractedProposition = qMatcher.group(1).trim();
            // Capitalize first letter
            if (extractedProposition.length() > 1) {
                extractedProposition = Character.toUpperCase(extractedProposition.charAt(0)) + extractedProposition.substring(1);
            }
            return ValidationResult.builder()
                    .isVerifiableClaim(true)
                    .extractedFactualClaim(extractedProposition)
                    .build();
        }

        if (endsWithQuestion && words.length < 4) {
            notes.add("Short question lacks sufficient factual context for verification.");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Inconclusive short question")
                    .advisoryNotes(notes)
                    .build();
        }

        return ValidationResult.builder()
                .isVerifiableClaim(true)
                .extractedFactualClaim(trimmed)
                .build();
    }
}
