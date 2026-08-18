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
            "^(what|who|why|where|when|how|is it true that|can you|could you|does|do|will|should|are there|is there)\\b",
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

        // 3. Check for pure questions / inquiries without a declarative proposition
        boolean isInterrogative = INTERROGATIVE_START_PATTERN.matcher(trimmed).find();
        boolean endsWithQuestion = trimmed.endsWith("?");

        if ((isInterrogative && words.length <= 7) || (endsWithQuestion && words.length <= 4)) {
            notes.add("Inquiries and questions ('What is...', 'Who is...') cannot be verified as true or false because they lack a declarative factual assertion.");
            notes.add("Try rephrasing as a statement (e.g., instead of 'Is Earth flat?', enter 'The Earth is flat and guarded by ice wall').");
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
