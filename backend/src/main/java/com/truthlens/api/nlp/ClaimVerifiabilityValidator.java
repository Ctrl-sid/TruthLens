package com.truthlens.api.nlp;

import com.truthlens.api.model.InputType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage 02 Hard Input-Type Classifier and Claim Verifiability Gate.
 * Strict Anti-Hallucination: Questions, opinions, greetings, instructions, and noise
 * are classified and blocked from triggering external search or receiving numeric truth scores.
 */
@Component
public class ClaimVerifiabilityValidator {

    private static final Set<String> COMMON_GREETINGS = Set.of(
            "hello", "hi", "hey", "good morning", "good evening", "good afternoon",
            "how are you", "who are you", "what are you", "tell me a joke", "thanks", "thank you",
            "test", "testing", "help", "ok", "okay", "bye", "goodbye", "yo", "sup"
    );

    private static final Pattern REPETITIVE_GIBBERISH_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9])\\1{4,}$|^[b-df-hj-np-tv-z]{6,}$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern QUESTION_START_PATTERN = Pattern.compile(
            "(?i)^(?:what|why|when|where|who|whom|whose|how|which|can|could|should|would|is|are|am|was|were|do|does|did|has|have|had|will|shall|may|might)\\b"
    );

    private static final Pattern REQUEST_INSTRUCTION_PATTERN = Pattern.compile(
            "(?i)^(?:what should i do|how (?:do|can|should) i|tell me|show me|explain (?:to me|how|why)|help me|guide me|give me|write (?:a|me)|advise me|suggest)\\b"
    );

    private static final Pattern OPINION_PATTERN = Pattern.compile(
            "(?i)^(?:i think|i believe|in my opinion|in my view|i feel|i agree|i disagree|to be honest|personally i think|i doubt)\\b|\\b(?:is (?:terrible|awful|awesome|amazing|disgusting|the best ever|the worst ever|so cool|great))\\b"
    );

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "(?i)^(?:run|stop|delete|calculate|execute|open|close|download|upload)\\b"
    );

    @Getter
    @Builder
    public static class ValidationResult {
        private InputType inputType;
        private boolean isVerifiableClaim;
        private boolean claimDetected;
        private String extractedFactualClaim; // Declarative claim text (null if non-claim)
        private String rejectionReason;
        private String suggestedAction;
        private List<String> advisoryNotes;
    }

    public ValidationResult validateClaimVerifiability(String text) {
        if (text == null || text.isBlank()) {
            return ValidationResult.builder()
                    .inputType(InputType.EMPTY_INPUT)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("Empty input payload")
                    .suggestedAction("Please provide a news headline, statement, or article URL to verify.")
                    .advisoryNotes(List.of("Please provide a news headline, statement, or article URL."))
                    .build();
        }

        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();

        // 0. URLs are directly verifiable via domain & wire lookups
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.matches("^(?i)(www\\.[a-zA-Z0-9-]+\\.[a-z]{2,}|[a-zA-Z0-9-]+\\.(com|org|net|edu|gov|io|in|co|uk|ai)/.*)")) {
            return ValidationResult.builder()
                    .inputType(InputType.URL_ARTICLE)
                    .isVerifiableClaim(true)
                    .claimDetected(true)
                    .extractedFactualClaim(trimmed)
                    .advisoryNotes(List.of())
                    .build();
        }

        String[] words = trimmed.split("\\s+");
        List<String> notes = new ArrayList<>();

        // 1. Check for single-character or keyboard gibberish / OCR noise
        int garbageChars = 0;
        for (char c : trimmed.toCharArray()) {
            if ("^~=\\/&_$%*#|{}[]<>`—".indexOf(c) >= 0) {
                garbageChars++;
            }
        }
        double garbageRatio = ((double) garbageChars / trimmed.length()) * 100.0;
        
        int singleLetterWords = 0;
        for (String w : words) {
            if (w.length() <= 1 && Character.isLetterOrDigit(w.charAt(0))) {
                singleLetterWords++;
            }
        }
        double singleLetterRatio = words.length > 0 ? ((double) singleLetterWords / words.length) * 100.0 : 0.0;

        if (garbageRatio > 15.0 || (words.length >= 4 && singleLetterRatio > 40.0) || REPETITIVE_GIBBERISH_PATTERN.matcher(trimmed).find() || (words.length == 1 && trimmed.length() > 15 && !trimmed.contains("."))) {
            notes.add("The input consists of fragmented characters, unreadable OCR tokens, or unstructured noise.");
            notes.add("TruthLens strictly prevents inventing or hallucinating a claim from unreadable text.");
            return ValidationResult.builder()
                    .inputType(InputType.PURE_NOISE)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("Unreadable, fragmented, or noisy text input")
                    .suggestedAction("Enter a clear, well-formed news headline or declarative statement.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 2. Check for casual greetings / chatbot conversational prompts
        if (COMMON_GREETINGS.contains(lower) || lower.matches("^(hi|hello|hey)\\b.*")) {
            notes.add("TruthLens is an automated verification engine for news assertions, not a conversational chatbot.");
            notes.add("Please enter a factual claim statement (e.g., 'NASA discovers atmospheric water vapor on exoplanet').");
            return ValidationResult.builder()
                    .inputType(InputType.GREETING)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("Conversational greeting or non-factual prompt detected")
                    .suggestedAction("Enter a factual news statement such as: 'The earthquake killed 95 people in Nepal.'")
                    .advisoryNotes(notes)
                    .build();
        }

        // 3. Check for Requests / Instructions (e.g. "what should i do", "tell me how to...")
        if (REQUEST_INSTRUCTION_PATTERN.matcher(trimmed).find()) {
            notes.add("The submitted text is an action request or personal advice inquiry, not a verifiable news proposition.");
            notes.add("TruthLens verifies declarative factual claims against accredited records.");
            return ValidationResult.builder()
                    .inputType(InputType.REQUEST)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("The submitted text is an open-ended request or instruction and does not contain a specific factual assertion")
                    .suggestedAction("Enter a declarative factual statement such as: 'A flood occurred in North Carolina on September 27.'")
                    .advisoryNotes(notes)
                    .build();
        }

        // 4. Check for Opinions / Subjective value judgments
        if (OPINION_PATTERN.matcher(trimmed).find()) {
            notes.add("Subjective viewpoints, emotional evaluations, and personal opinions cannot be verified as true or false.");
            notes.add("TruthLens evaluates objective, testable factual propositions.");
            return ValidationResult.builder()
                    .inputType(InputType.OPINION)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("Subjective opinion or personal judgment detected")
                    .suggestedAction("Provide an objective factual statement instead of a personal opinion.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 5. Check for Commands
        if (words.length <= 3 && COMMAND_PATTERN.matcher(trimmed).find() && !trimmed.contains(".")) {
            notes.add("Imperative system commands cannot be verified as true or false.");
            return ValidationResult.builder()
                    .inputType(InputType.COMMAND)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("Imperative command lacking a factual assertion")
                    .suggestedAction("Enter a factual news claim to verify.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 6. Check for single word inputs without predicate
        if (words.length == 1 && !trimmed.contains(".")) {
            notes.add("A single word without an event predicate cannot be verified as true or false.");
            return ValidationResult.builder()
                    .inputType(InputType.PURE_NOISE)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("Single word input lacking factual predicate")
                    .suggestedAction("Enter a complete statement with a subject, action, and context.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 7. Check for WHO organization vs Interrogative Questions
        boolean endsWithQuestion = trimmed.endsWith("?");
        boolean isWhoOrgHeadline = (trimmed.startsWith("WHO ") || trimmed.startsWith("W.H.O. ")) && !endsWithQuestion &&
                (lower.contains("approves") || lower.contains("declares") || lower.contains("publishes") || lower.contains("warns") ||
                 lower.contains("reports") || lower.contains("confirms") || lower.contains("recommends") || lower.contains("states") ||
                 lower.contains("guidelines") || lower.contains("vaccine") || lower.contains("health") || lower.contains("officially"));

        if (isWhoOrgHeadline) {
            return ValidationResult.builder()
                    .inputType(InputType.FACTUAL_CLAIM)
                    .isVerifiableClaim(true)
                    .claimDetected(true)
                    .extractedFactualClaim(trimmed)
                    .advisoryNotes(List.of())
                    .build();
        }

        // 8. Strict Question Detection (Wh-words, Modal verbs, Inversion, Question Marks)
        // STRICT ANTI-HALLUCINATION RULE: Questions are NOT automatically rewritten to claims.
        boolean startsWithQuestionWord = QUESTION_START_PATTERN.matcher(trimmed).find();
        if (endsWithQuestion || startsWithQuestionWord) {
            notes.add("TruthLens verifies factual assertions, but this input is an open-ended or interrogative question and does not contain a declarative factual claim.");
            notes.add("Questions cannot be automatically converted into claims because doing so can lead to hallucinated verifications.");
            return ValidationResult.builder()
                    .inputType(InputType.QUESTION)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .rejectionReason("The submitted text is a question and does not contain a specific factual assertion that can be verified")
                    .suggestedAction("Enter a declarative factual statement (e.g., 'The earthquake killed 95 people in Nepal.').")
                    .advisoryNotes(notes)
                    .build();
        }

        // 9. Standard Factual Claim
        return ValidationResult.builder()
                .inputType(InputType.FACTUAL_CLAIM)
                .isVerifiableClaim(true)
                .claimDetected(true)
                .extractedFactualClaim(trimmed)
                .advisoryNotes(List.of())
                .build();
    }
}
