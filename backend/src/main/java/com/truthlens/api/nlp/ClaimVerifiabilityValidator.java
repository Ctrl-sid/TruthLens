package com.truthlens.api.nlp;

import com.truthlens.api.dto.ClaimVerificationResponse.ClaimFingerprint;
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
 * Strict Anti-Hallucination: Questions, opinions, greetings, instructions, fragments, and noise
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

    // Explicit non-verifiable fragments, search terms, and meta-commentary
    private static final Pattern NON_VERIFIABLE_FRAGMENT_PATTERN = Pattern.compile(
            "(?i)^(?:fake news|real news|true news|news|news\\?|real\\?|fake\\?|this is fake|this is definitely fake|" +
            "i think this news is fake|i think this is fake|is this news true|is this true|is it true|is this real|" +
            "is it real|true or false|fact check|factcheck|debunk this|verify this|check this|breaking|breaking news|" +
            "omg|wow|help|what|why|who|hoax|rumor|scam|clickbait|fake|true|false)$"
    );

    private static final Pattern META_COMMENTARY_PATTERN = Pattern.compile(
            "(?i)^(?:(?:i think|i believe|personally|in my opinion|people say)\\s+)?(?:this|that|the news|it)\\s+(?:is|looks|seems)\\s+(?:definitely\\s+)?(?:fake|false|true|real|a hoax|a lie|misleading)$"
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
            "(?i)^(?:run|stop|delete|calculate|execute|open|close|download|upload|search|find)\\b"
    );

    @Getter
    @Builder
    public static class ValidationResult {
        private InputType inputType;
        private boolean isVerifiableClaim;
        private boolean claimDetected;
        private String extractedFactualClaim; // Declarative claim text (null if non-claim)
        private String claimType; // GOVERNMENT_ACTION, EVENT_OCCURRENCE, CASUALTY_COUNT, SCIENTIFIC, POLICY, etc.
        private double extractionConfidence; // 0.0 to 1.0
        private boolean verificationEligible;
        private ClaimFingerprint fingerprint;
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
                    .claimType(null)
                    .extractionConfidence(1.0)
                    .verificationEligible(false)
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
                    .claimType("URL_REFERENCE")
                    .extractionConfidence(0.98)
                    .verificationEligible(true)
                    .fingerprint(ClaimFingerprint.builder().subject("URL Article").action("reported").objectValue(trimmed).build())
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
                    .inputType(InputType.OCR_UNRELIABLE)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.95)
                    .verificationEligible(false)
                    .rejectionReason("Unreadable, fragmented, or noisy text input")
                    .suggestedAction("Enter a clear, well-formed news headline or declarative statement.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 2. Check for Non-Verifiable Fragments and Meta-Commentary (e.g. "fake news", "real?", "is this true", "this is fake")
        if (NON_VERIFIABLE_FRAGMENT_PATTERN.matcher(trimmed).matches() || META_COMMENTARY_PATTERN.matcher(trimmed).matches()) {
            notes.add("The submitted text does not specify an actionable factual claim to verify.");
            notes.add("TruthLens evaluates declarative factual news propositions against accredited records rather than generic search tags or meta-comments.");
            return ValidationResult.builder()
                    .inputType(InputType.NON_VERIFIABLE_FRAGMENT)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.99)
                    .verificationEligible(false)
                    .rejectionReason("The submitted text does not specify a factual claim that can be independently verified")
                    .suggestedAction("Paste the actual headline, claim, article URL, or upload the image.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 3. Check for casual greetings / chatbot conversational prompts
        if (COMMON_GREETINGS.contains(lower) || lower.matches("^(hi|hello|hey)\\b.*")) {
            notes.add("TruthLens is an automated verification engine for news assertions, not a conversational chatbot.");
            notes.add("Please enter a factual claim statement (e.g., 'NASA discovers atmospheric water vapor on exoplanet').");
            return ValidationResult.builder()
                    .inputType(InputType.NON_VERIFIABLE_META_TEXT)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.98)
                    .verificationEligible(false)
                    .rejectionReason("Conversational greeting or non-factual prompt detected")
                    .suggestedAction("Enter a factual news statement such as: 'The earthquake killed 95 people in Nepal.'")
                    .advisoryNotes(notes)
                    .build();
        }

        // 4. Check for Requests / Instructions (e.g. "what should i do", "tell me how to...")
        if (REQUEST_INSTRUCTION_PATTERN.matcher(trimmed).find()) {
            notes.add("The submitted text is an action request or personal advice inquiry, not a verifiable news proposition.");
            notes.add("TruthLens verifies declarative factual claims against accredited records.");
            return ValidationResult.builder()
                    .inputType(InputType.NON_VERIFIABLE_QUESTION)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.99)
                    .verificationEligible(false)
                    .rejectionReason("The submitted text is a question/advice request rather than a factual assertion that can be independently verified")
                    .suggestedAction("Enter a declarative factual statement such as: 'India dispatched relief materials to Nepal.'")
                    .advisoryNotes(notes)
                    .build();
        }

        // 5. Check for Opinions / Subjective value judgments
        if (OPINION_PATTERN.matcher(trimmed).find() && !trimmed.contains(" reported ") && !trimmed.contains(" confirmed ")) {
            notes.add("Subjective viewpoints, emotional evaluations, and personal opinions cannot be verified as true or false.");
            notes.add("TruthLens evaluates objective, testable factual propositions.");
            return ValidationResult.builder()
                    .inputType(InputType.NON_VERIFIABLE_OPINION)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.92)
                    .verificationEligible(false)
                    .rejectionReason("Subjective opinion or personal judgment detected")
                    .suggestedAction("Provide an objective factual statement instead of a personal opinion.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 6. Check for Commands
        if (words.length <= 3 && COMMAND_PATTERN.matcher(trimmed).find() && !trimmed.contains(".")) {
            notes.add("Imperative system commands cannot be verified as true or false.");
            return ValidationResult.builder()
                    .inputType(InputType.NON_VERIFIABLE_COMMAND)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.95)
                    .verificationEligible(false)
                    .rejectionReason("Imperative command lacking a factual assertion")
                    .suggestedAction("Enter a factual news claim to verify.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 7. Check for single word inputs or short 2-word fragments without predicate
        if (words.length <= 2 && !trimmed.contains(".") && !trimmed.contains("?") && !trimmed.matches("(?i).*(killed|sent|cures|discovers|earthquake|flood).*")) {
            notes.add("A short fragment lacking a factual predicate cannot be verified as true or false.");
            return ValidationResult.builder()
                    .inputType(InputType.NON_VERIFIABLE_FRAGMENT)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.96)
                    .verificationEligible(false)
                    .rejectionReason("Incomplete text fragment lacking an actionable event predicate")
                    .suggestedAction("Enter a complete statement with a subject, action, and context.")
                    .advisoryNotes(notes)
                    .build();
        }

        // 8. Check for WHO organization vs Interrogative Questions
        boolean endsWithQuestion = trimmed.endsWith("?");
        boolean isWhoOrgHeadline = (trimmed.startsWith("WHO ") || trimmed.startsWith("W.H.O. ")) && !endsWithQuestion &&
                (lower.contains("approves") || lower.contains("declares") || lower.contains("publishes") || lower.contains("warns") ||
                 lower.contains("reports") || lower.contains("confirms") || lower.contains("recommends") || lower.contains("states") ||
                 lower.contains("guidelines") || lower.contains("vaccine") || lower.contains("health") || lower.contains("officially"));

        if (isWhoOrgHeadline) {
            ClaimFingerprint fp = extractFingerprint(trimmed, "SCIENTIFIC_POLICY");
            return ValidationResult.builder()
                    .inputType(InputType.VERIFIABLE_CLAIM)
                    .isVerifiableClaim(true)
                    .claimDetected(true)
                    .extractedFactualClaim(trimmed)
                    .claimType("SCIENTIFIC_POLICY")
                    .extractionConfidence(0.96)
                    .verificationEligible(true)
                    .fingerprint(fp)
                    .advisoryNotes(List.of())
                    .build();
        }

        // 9. Strict Question Detection (Wh-words, Modal verbs, Inversion, Question Marks)
        // STRICT ANTI-HALLUCINATION RULE: Questions are NOT automatically rewritten to claims.
        boolean startsWithQuestionWord = QUESTION_START_PATTERN.matcher(trimmed).find();
        if (endsWithQuestion || startsWithQuestionWord) {
            notes.add("TruthLens verifies factual assertions, but this input is an open-ended or interrogative question and does not contain a declarative factual claim.");
            notes.add("Questions cannot be automatically converted into claims because doing so can lead to hallucinated verifications.");
            return ValidationResult.builder()
                    .inputType(InputType.NON_VERIFIABLE_QUESTION)
                    .isVerifiableClaim(false)
                    .claimDetected(false)
                    .extractedFactualClaim(null)
                    .claimType(null)
                    .extractionConfidence(0.99)
                    .verificationEligible(false)
                    .rejectionReason("The submitted text is a question and does not contain a specific factual assertion that can be verified")
                    .suggestedAction("Enter a declarative factual statement (e.g., 'India dispatched relief materials to Nepal.').")
                    .advisoryNotes(notes)
                    .build();
        }

        // 10. Standard Verifiable Factual Claim with Structured Fingerprint
        String claimType = determineClaimType(trimmed);
        ClaimFingerprint fingerprint = extractFingerprint(trimmed, claimType);

        return ValidationResult.builder()
                .inputType(InputType.VERIFIABLE_CLAIM)
                .isVerifiableClaim(true)
                .claimDetected(true)
                .extractedFactualClaim(trimmed)
                .claimType(claimType)
                .extractionConfidence(0.95)
                .verificationEligible(true)
                .fingerprint(fingerprint)
                .advisoryNotes(List.of())
                .build();
    }

    private String determineClaimType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("killed") || lower.contains("dead") || lower.contains("fatalities") || lower.contains("casualties") || lower.contains("death toll")) {
            return "CASUALTY_COUNT";
        }
        if (lower.contains("relief") || lower.contains("dispatched") || lower.contains("sent aid") || lower.contains("aid materials") || lower.contains("rescue")) {
            return "GOVERNMENT_ACTION";
        }
        if (lower.contains("nasa") || lower.contains("telescope") || lower.contains("exoplanet") || lower.contains("cancer") || lower.contains("cure") || lower.contains("vaccine") || lower.contains("5g")) {
            return "SCIENTIFIC";
        }
        if (lower.contains("earthquake") || lower.contains("flood") || lower.contains("cyclone") || lower.contains("landslide") || lower.contains("erupted") || lower.contains("storm")) {
            return "EVENT_OCCURRENCE";
        }
        if (lower.contains("policy") || lower.contains("ban") || lower.contains("guidelines") || lower.contains("ordinance") || lower.contains("law")) {
            return "POLICY";
        }
        if (lower.contains("won") || lower.contains("match") || lower.contains("tournament") || lower.contains("cup") || lower.contains("finals")) {
            return "SPORTS_RESULT";
        }
        return "EVENT_OCCURRENCE";
    }

    private ClaimFingerprint extractFingerprint(String text, String claimType) {
        String subject = "Unspecified Subject";
        String action = "asserted";
        String objectValue = text;
        String location = null;
        String event = null;
        String eventType = claimType;
        String time = null;

        String lower = text.toLowerCase();
        if (lower.contains("nepal")) location = "Nepal";
        else if (lower.contains("india")) location = "India";
        else if (lower.contains("north carolina")) location = "North Carolina";
        else if (lower.contains("london")) location = "London";

        if (lower.contains("flood")) event = "Flood";
        else if (lower.contains("earthquake")) event = "Earthquake";
        else if (lower.contains("cyclone")) event = "Cyclone";
        else if (lower.contains("space")) event = "Astronomical Discovery";

        String[] words = text.split("\\s+");
        if (words.length >= 3) {
            subject = words[0];
            if (words.length > 1 && Character.isUpperCase(words[1].charAt(0))) {
                subject += " " + words[1];
            }
            action = words.length > 2 ? words[1] + " " + words[2] : words[1];
        }

        return ClaimFingerprint.builder()
                .subject(subject)
                .action(action)
                .objectValue(objectValue)
                .location(location)
                .event(event)
                .eventType(eventType)
                .time(time)
                .build();
    }
}

