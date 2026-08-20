package com.truthlens.api.nlp;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ClaimVerifiabilityValidator {

    private static final Set<String> COMMON_GREETINGS = Set.of(
            "hello", "hi", "hey", "good morning", "good evening", "good afternoon",
            "how are you", "who are you", "what are you", "tell me a joke", "thanks", "thank you",
            "test", "testing", "help", "ok", "okay", "bye", "goodbye"
    );

    private static final Pattern INTERROGATIVE_START_PATTERN = Pattern.compile(
            "^(what|who|why|where|when|how|is|are|was|were|did|do|does|has|have|had|can|could|will|would|should|may|might|is it true that)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REPETITIVE_GIBBERISH_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9])\\1{4,}$|^[b-df-hj-np-tv-z]{6,}$",
            Pattern.CASE_INSENSITIVE
    );

    @Getter
    @Builder
    public static class ValidationResult {
        private boolean isVerifiableClaim;
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
                    .build();
        }

        String[] words = trimmed.split("\\s+");
        List<String> notes = new ArrayList<>();

        // 1. Check for casual greetings / chatbot conversational prompts
        if (COMMON_GREETINGS.contains(lower) || lower.matches("^(hi|hello|hey)\\b.*")) {
            notes.add("TruthLens is an automated fact-checking engine for news claims, not a conversational chatbot.");
            notes.add("Please enter a factual claim statement (e.g., 'NASA discovers atmospheric water vapor on exoplanet').");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Conversational greeting or prompt detected")
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

        // 3. Check for questions / inquiries without a declarative proposition
        boolean endsWithQuestion = trimmed.endsWith("?");
        boolean isWhoOrgHeadline = (trimmed.startsWith("WHO ") || trimmed.startsWith("W.H.O. ")) && !endsWithQuestion &&
                (lower.contains("approves") || lower.contains("declares") || lower.contains("publishes") || lower.contains("warns") ||
                 lower.contains("reports") || lower.contains("confirms") || lower.contains("recommends") || lower.contains("states") ||
                 lower.contains("guidelines") || lower.contains("vaccine") || lower.contains("health") || lower.contains("officially"));

        boolean isInterrogative = !isWhoOrgHeadline && (
                lower.startsWith("what ") || lower.startsWith("why ") || lower.startsWith("where ") ||
                lower.startsWith("when ") || lower.startsWith("how ") || lower.startsWith("is it true that") ||
                lower.startsWith("did ") || lower.startsWith("does ") || lower.startsWith("do ") ||
                lower.startsWith("was ") || lower.startsWith("were ") || lower.startsWith("can ") ||
                lower.startsWith("could ") || lower.startsWith("will ") || lower.startsWith("would ") ||
                lower.startsWith("should ") || (lower.startsWith("who ") && (lower.startsWith("who is") || lower.startsWith("who was") || lower.startsWith("who did") || lower.startsWith("who won") || lower.startsWith("who are")))
        );

        if (isInterrogative || endsWithQuestion) {
            notes.add("Inquiries and questions ('Did the...', 'What is...', 'Is X...') cannot be verified as true or false because they lack a declarative factual assertion.");
            notes.add("Try rephrasing as a declarative news statement or headline (e.g., instead of 'Did the Prime Minister pass away?', enter 'Prime Minister passed away according to official statement').");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Interrogative question or inquiry (lacks declarative factual assertion)")
                    .advisoryNotes(notes)
                    .build();
        }

        // 4. Check for insufficient length / single word input
        if (words.length < 3 || trimmed.length() < 12) {
            notes.add("Input is too brief (" + words.length + " word" + (words.length == 1 ? "" : "s") + ") to extract meaningful entities and context for wire cross-referencing.");
            notes.add("A complete news claim typically requires at least 4–5 words with a subject and assertion.");
            return ValidationResult.builder()
                    .isVerifiableClaim(false)
                    .rejectionReason("Input is too short to constitute a verifiable news claim")
                    .advisoryNotes(notes)
                    .build();
        }

        return ValidationResult.builder()
                .isVerifiableClaim(true)
                .build();
    }
}
