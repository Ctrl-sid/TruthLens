package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimVerificationRequest;
import com.truthlens.api.dto.ClaimVerificationResponse;
import com.truthlens.api.nlp.*;
import com.truthlens.api.repository.VerifiedSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class FactCheckEngineServiceTest {

    private FactCheckEngineService factCheckEngineService;
    private VerifiedSourceRepository verifiedSourceRepository;
    private ExternalFactCheckService externalFactCheckService;

    @BeforeEach
    public void setup() {
        TfIdfVectoriser tfIdfVectoriser = new TfIdfVectoriser();
        CosineSimilarityEngine cosineSimilarityEngine = new CosineSimilarityEngine();
        FactCheckingCorpus factCheckingCorpus = new FactCheckingCorpus(tfIdfVectoriser, cosineSimilarityEngine);
        factCheckingCorpus.init();

        NamedEntityExtractor namedEntityExtractor = new NamedEntityExtractor();
        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
        ClickbaitClassifier clickbaitClassifier = new ClickbaitClassifier();
        NlpPipelineService nlpPipelineService = new NlpPipelineService(namedEntityExtractor, sentimentAnalyzer, clickbaitClassifier);

        OcrAnalysisService ocrAnalysisService = new OcrAnalysisService();
        ClaimVerifiabilityValidator claimVerifiabilityValidator = new ClaimVerifiabilityValidator();

        verifiedSourceRepository = Mockito.mock(VerifiedSourceRepository.class);
        externalFactCheckService = Mockito.mock(ExternalFactCheckService.class);
        when(externalFactCheckService.queryExternalKnowledge(anyString())).thenReturn(Optional.empty());

        factCheckEngineService = new FactCheckEngineService(
                nlpPipelineService,
                ocrAnalysisService,
                factCheckingCorpus,
                externalFactCheckService,
                verifiedSourceRepository,
                claimVerifiabilityValidator
        );
    }

    @Test
    @DisplayName("Interrogative Question Input should return NON-VERIFIABLE INPUT with null score (N/A)")
    public void testQuestionInput() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("what is this?")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("NON-VERIFIABLE") || response.getVerdict().contains("NOT_VERIFIABLE"));
        assertNull(response.getGenuinenessScore(), "Non-verifiable input should have null score (rendered as N/A)");
        assertNull(response.getSupportScore());
        assertTrue(response.getRationale().toLowerCase().contains("does not constitute") || response.getRationale().toLowerCase().contains("lack"));
    }

    @Test
    @DisplayName("Question with question word should return NON-VERIFIABLE INPUT with null score (N/A)")
    public void testFactualQuestionExtraction() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Did India send relief materials to Nepal?")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("NON-VERIFIABLE"));
        assertNull(response.getGenuinenessScore(), "Question must receive null score (N/A)");
        assertNull(response.getSupportScore());
        assertEquals("BLOCKED", response.getPipelineStatus());
        assertTrue(response.getInputType().contains("QUESTION"));
    }

    @Test
    @DisplayName("Single Word Input should return NON-VERIFIABLE INPUT with null score (N/A)")
    public void testSingleWordInput() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("apple")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("NON-VERIFIABLE") || response.getVerdict().contains("NOT_VERIFIABLE"));
        assertNull(response.getGenuinenessScore(), "Non-verifiable input should have null score (rendered as N/A)");
    }

    @Test
    @DisplayName("Conversational Greeting should return NON-VERIFIABLE INPUT with null score (N/A)")
    public void testGreetingInput() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("hello how are you")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("NON-VERIFIABLE") || response.getVerdict().contains("NOT_VERIFIABLE"));
        assertNull(response.getGenuinenessScore(), "Non-verifiable input should have null score (rendered as N/A)");
    }

    @Test
    @DisplayName("Debunked Health Fake News should return STRONGLY CONTRADICTED / DOCUMENTED_HOAX with score <= 25")
    public void testCancerCureFakeNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Drinking boiled lemon water and baking soda cures cancer in 48 hours without chemotherapy!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("CONTRADICTED") || response.getVerdict().contains("HOAX") || response.getVerdict().contains("FABRICATED"));
        assertFalse(response.getSources().isEmpty());
        assertTrue(response.getRationale().toLowerCase().contains("cancer") || response.getRationale().toLowerCase().contains("debunked") || response.getRationale().toLowerCase().contains("chemotherapy"));
    }

    @Test
    @DisplayName("5G Conspiracy Fake News should return STRONGLY CONTRADICTED / DOCUMENTED_HOAX with score <= 25")
    public void test5gConspiracyFakeNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("5G wireless towers are emitting radiation that creates covid viruses to control human brains!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("CONTRADICTED") || response.getVerdict().contains("HOAX") || response.getVerdict().contains("FABRICATED"));
    }

    @Test
    @DisplayName("Unverified Arbitrary Rumor should receive INSUFFICIENT EVIDENCE with null score (Score = N/A)")
    public void testUnverifiedArbitraryClaim() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("A local mayor was spotted secretly purchasing a naval submarine in international waters yesterday.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("INSUFFICIENT EVIDENCE", response.getVerdict());
        assertNull(response.getGenuinenessScore(), "Score must be null (N/A) for INSUFFICIENT EVIDENCE");
        assertNull(response.getSupportScore(), "Support score must be null (N/A) for INSUFFICIENT EVIDENCE");
        assertEquals("LOW", response.getConfidence());
    }

    @Test
    @DisplayName("Verified NASA Discovery should score >= 80 and return VERIFIED / STRONGLY SUPPORTED")
    public void testVerifiedNasaNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("NASA James Webb Space Telescope discovers atmospheric water vapor on distant exoplanet LHS 1140b.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("SUPPORTED"));
    }

    @Test
    @DisplayName("Verified WHO Guidelines should score >= 80 and return VERIFIED / STRONGLY SUPPORTED")
    public void testVerifiedWhoNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("World Health Organization publishes updated clinical guidelines on seasonal viral mitigation and vaccination schedules.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("SUPPORTED"));
    }

    @Test
    @DisplayName("Debunked Flat Earth Hoax should return STRONGLY CONTRADICTED / DOCUMENTED_HOAX with score <= 25")
    public void testFlatEarthHoax() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("The Earth is flat and the Antarctic ice wall is guarded by United Nations warships to hide the truth!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("CONTRADICTED") || response.getVerdict().contains("HOAX") || response.getVerdict().contains("FABRICATED"));
    }

    @Test
    @DisplayName("Debunked Vaccine Microchip Conspiracy should return STRONGLY CONTRADICTED / DOCUMENTED_HOAX with score <= 25")
    public void testVaccineMicrochipConspiracy() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("COVID-19 vaccines contain microscopic tracking microchips and nanobots for government population surveillance!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("CONTRADICTED") || response.getVerdict().contains("HOAX") || response.getVerdict().contains("FABRICATED"));
    }

    @Test
    @DisplayName("Debunked World Bank Debt Forgiveness Scam should return STRONGLY CONTRADICTED / DOCUMENTED_HOAX with score <= 25")
    public void testWorldBankDebtScam() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("World Bank announced all national personal debts and mortgages will be wiped clean by Friday!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("CONTRADICTED") || response.getVerdict().contains("HOAX") || response.getVerdict().contains("FABRICATED"));
    }

    @Test
    @DisplayName("Verified CRISPR Gene Therapy should score >= 80 and return VERIFIED / STRONGLY SUPPORTED")
    public void testVerifiedCrisprNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("FDA approves breakthrough CRISPR Cas9 gene editing therapeutic Casgevy for treatment of sickle cell disease.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("SUPPORTED"));
    }

    @Test
    @DisplayName("Question 'Did the Prime Minister pass away?' should extract proposition and evaluate")
    public void testDidPrimeMinisterPassAwayQuestion() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Did the Prime Minister pass away?")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("NON-VERIFIABLE") || response.getVerdict().contains("INSUFFICIENT"));
        assertNull(response.getGenuinenessScore(), "Question must receive null score (N/A)");
    }

    @Test
    @DisplayName("Verified Queen Elizabeth II passing should return VERIFIED / STRONGLY SUPPORTED with score >= 80")
    public void testQueenElizabethPassingVerified() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Queen Elizabeth II passed away at Balmoral on September 8, 2022")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("SUPPORTED"));
    }

    @Test
    @DisplayName("Unverified rumor about PM Modi passing should NOT match Queen Elizabeth and should return INSUFFICIENT EVIDENCE with null score")
    public void testUnverifiedModiDeathRumor() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Indian PM Narendra Modi Passed away")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertNull(response.getGenuinenessScore(), "Score must be null (N/A)");
        assertFalse(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("STRONGLY SUPPORTED"));
        assertFalse(response.getRationale().toLowerCase().contains("queen elizabeth"), "Rationale must NOT contain Queen Elizabeth!");
    }

    @Test
    @DisplayName("Genuine news reported in The Hindu/wires 'Kolkata hotel fire kills 9 Bangladeshi nationals' should verify as genuine")
    public void testKolkataHotelFireGenuineNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Kolkata hotel fire kills 9 Bangladeshi nationals")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 75, "Genuine wire news should score >= 75, was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("SUPPORTED"), "Verdict should contain GENUINE or SUPPORTED, was: " + response.getVerdict());
    }

    @Test
    @DisplayName("Demographic impossibility 'there are 100 billion humans in earth' should be flagged as false/misleading")
    public void testDemographicAnomalyPopulation() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("there are 100 billion humans in earth")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 35, "Score should be <= 35 for 100 billion humans, was: " + response.getGenuinenessScore());
        assertFalse(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("STRONGLY SUPPORTED"));
        assertTrue(response.getRationale().toLowerCase().contains("8.1 billion") || response.getRationale().toLowerCase().contains("population"), "Rationale should explain demographic limit!");
    }

    @Test
    @DisplayName("Image verification with explicit title should evaluate the explicit title")
    public void testImageVerificationWithExplicitTitle() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("IMAGE")
                .content("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDcuresecretfake1234567890")
                .title("Kolkata hotel fire kills 9 Bangladeshi nationals")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 75, "Score should be >= 75 for verified title, was: " + response.getGenuinenessScore());
        assertNotNull(response.getImageAnalysis());
        assertFalse(response.getRationale().toLowerCase().contains("miracle cure"), "Must NOT trigger accidental miracle cure fallback!");
    }

    @Test
    @DisplayName("Altered news 'massive fire in kolkata killed none' should be flagged as contradiction / fake")
    public void testAlteredNewsContradictionKilledNone() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("massive fire in kolkata killed none")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 30, "Altered news with 0 casualties should score <= 30, was: " + response.getGenuinenessScore());
        assertFalse(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("STRONGLY SUPPORTED"), "Verdict must not be genuine!");
        assertTrue(response.getRationale().toLowerCase().contains("contradict") || response.getRationale().toLowerCase().contains("none"), "Rationale should explain contradiction!");
    }

    @Test
    @DisplayName("Genuine news 'massive fire in kolkata killed nine' should score >= 75")
    public void testGenuineNewsKilledNine() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("massive fire in kolkata killed nine")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 75, "Genuine news should score >= 75, was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE") || response.getVerdict().contains("SUPPORTED"), "Verdict must be GENUINE or SUPPORTED!");
    }

    @Test
    @DisplayName("News starting with WHO acronym should NOT be rejected as a non-verifiable question")
    public void testWhoOrganizationAcronymNotRejected() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("WHO approves updated malaria vaccine guidelines for high-transmission regions")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertNotEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertTrue(response.getClaimDetected());
        assertTrue(response.getVerificationEligible());
    }

    @Test
    @DisplayName("Valid URL input should NOT be rejected as input too short")
    public void testUrlInputVerifiability() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("URL")
                .content("https://thehindu.com/news/national/isro-gaganyaan-space-mission-launch-update/article123.ece")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getPipelineStatus());
        assertTrue(response.getClaimDetected());
    }

    @Test
    @DisplayName("Genuine claim should discover originating publisher and AUTHENTIC_REPRODUCTION provenance")
    public void testOriginDiscoveryGenuineNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("massive fire in kolkata killed nine")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertNotNull(response.getOriginDiscovery(), "Origin discovery metadata should be present!");
        assertEquals("AUTHENTIC_REPRODUCTION", response.getOriginDiscovery().getProvenanceType());
        assertNotNull(response.getOriginDiscovery().getOriginalPublisher());
        assertNotNull(response.getOriginDiscovery().getOriginalHeadline());
    }

    @Test
    @DisplayName("Altered claim should identify origin and flag ALTERED_DISTORTION with distortion details")
    public void testOriginDiscoveryAlteredNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("massive fire in kolkata killed none")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertNotNull(response.getOriginDiscovery(), "Origin discovery metadata should be present!");
        assertEquals("ALTERED_DISTORTION", response.getOriginDiscovery().getProvenanceType());
        assertNotNull(response.getOriginDiscovery().getOriginalPublisher());
        assertTrue(response.getOriginDiscovery().getDistortionAnalysis().toLowerCase().contains("none") ||
                   response.getOriginDiscovery().getDistortionAnalysis().toLowerCase().contains("casualties"));
    }

    @Test
    @DisplayName("Compound claim should be decomposed into atomic sub-claims")
    public void testClaimDecomposition() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("The Mumbai attack killed 0 people and happened in 2008.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertNotNull(response.getSubClaims());
        assertTrue(response.getSubClaims().size() >= 2, "Should decompose compound claim into at least 2 sub-claims!");
        assertNotNull(response.getExplainability());
        assertNotNull(response.getContentDiagnostics());
    }

    @Test
    @DisplayName("Uncorroborated emerging claim with no contradictory or confirming evidence should return INSUFFICIENT_EVIDENCE")
    public void testInsufficientEvidenceState() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("A small local bakery in Springfield introduced a new sourdough recipe today.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("INSUFFICIENT"));
        assertEquals("#94A3B8", response.getVerdictBadgeColor());
        assertNotNull(response.getExplainability());
        assertTrue(response.getExplainability().getWarningChecklist().stream()
                .anyMatch(w -> w.toLowerCase().contains("no primary wire") || w.toLowerCase().contains("emerging")));
    }

    @Test
    @DisplayName("Corrupted OCR in news screenshot should be assessed, normalized, reconstructed and verified")
    public void testImageVerificationWithCorruptedOcrReconstruction() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("IMAGE")
                .content("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP...")
                .title("NOVAK DJOKO!C OPENING ROUNO OF US OPEN FALLING IN 5 SETS TC MARIANO NAVC")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertNotNull(response.getImageAnalysis());
        assertEquals("USER_CORRECTED_OCR", response.getImageAnalysis().getClaimVerificationBasis());
        assertNotNull(response.getImageAnalysis().getReconstructedClaim());
        assertTrue(response.getImageAnalysis().getReconstructedClaim().toLowerCase().contains("novak djokovic"));
        assertTrue(response.getImageAnalysis().getReconstructedClaim().toLowerCase().contains("mariano navone"));
        assertEquals("NO_SIGNIFICANT_ANOMALY", response.getImageAnalysis().getForensicAssessment());
        assertNotNull(response.getImageAnalysis().getOcrQualityLevel());
        assertNotNull(response.getImageAnalysis().getForensicDisclaimer());
    }

    @Test
    @DisplayName("Pure photograph with no extractable text should return NON-VERIFIABLE IMAGE with NO_TEXT_DETECTED")
    public void testPurePhotographNoClaim() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("IMAGE")
                .content("")
                .title("")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE IMAGE", response.getVerdict());
        assertNull(response.getSupportScore());
        assertNull(response.getGenuinenessScore());
        assertNotNull(response.getImageAnalysis());
        assertEquals("NO_TEXT_DETECTED", response.getImageAnalysis().getClaimExtractionStatus());
        assertEquals("TEXT_ABSENT", response.getImageAnalysis().getTextPresence());
        assertTrue(response.getImageAnalysis().isRequiresUserReview());
    }

    @Test
    @DisplayName("Social media post with non-declarative prayer/appeal should triage as AMBIGUOUS_SOCIAL_POST with null score")
    public void testAmbiguousSocialMediaPost() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("IMAGE")
                .content("Please keep North Carolina and Tennessee in your prayers as unprecedented flood waters surge...")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("AMBIGUOUS") || response.getVerdict().contains("INSUFFICIENT"));
        assertNull(response.getGenuinenessScore(), "Ambiguous social post should have null score (N/A)");
        assertNull(response.getSupportScore());
        assertEquals("LOW", response.getConfidence());
        assertNotNull(response.getExplicitClaimText());
        assertNotNull(response.getInferredContext());
        assertNotNull(response.getVisualContextDescription());
        assertFalse(response.getClaimDisambiguationOptions().isEmpty(), "Should offer interactive disambiguation options");
        assertNotNull(response.getRetrievalQuality());
        assertEquals(10, response.getPipelineSteps().size(), "Should have full 10-stage execution pipeline trace");
    }

    @Test
    @DisplayName("Contradicted claim should have baseSupportScore and explicit contradictionPenalty")
    public void testContradictedClaimScoringBreakdown() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("massive fire in kolkata killed none")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertNotNull(response.getBaseSupportScore());
        assertNotNull(response.getContradictionPenalty());
        assertTrue(response.getContradictionPenalty() > 0, "Contradiction penalty should be > 0");
        assertEquals(10, response.getPipelineSteps().size());
        assertNotNull(response.getRetrievalQuality());
    }

    @Test
    @DisplayName("TEST 01 / TEST A: 'what should i do' MUST return NON-VERIFIABLE INPUT with Score=N/A and 0 searches")
    public void testRegression01WhatShouldIDo() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("what should i do")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertNull(response.getGenuinenessScore(), "Score must be null (rendered as N/A)");
        assertNull(response.getSupportScore(), "Support score must be null (rendered as N/A)");
        assertEquals("HIGH", response.getConfidence());
        assertNull(response.getConfidenceScore());
        assertEquals("BLOCKED", response.getPipelineStatus());
        assertEquals(false, response.getClaimDetected());
        assertEquals(false, response.getVerifiable());
        assertTrue(response.getSources().isEmpty(), "Evidence sources must be empty");
        assertEquals(10, response.getPipelineSteps().size());
        assertEquals("NOT_EXECUTED", response.getPipelineSteps().get(4).getStatus()); // Stage 05 Evidence Retrieval
    }

    @Test
    @DisplayName("TEST 02 / TEST B: 'why is the sky blue?' and 'Is the Earth round?' MUST classify as QUESTION and block verification")
    public void testRegression02QuestionDetection() {
        ClaimVerificationRequest request1 = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("why is the sky blue?")
                .build();
        ClaimVerificationResponse response1 = factCheckEngineService.verifyClaim(request1);
        assertEquals("NON-VERIFIABLE INPUT", response1.getVerdict());
        assertEquals("BLOCKED", response1.getPipelineStatus());
        assertNull(response1.getGenuinenessScore());

        ClaimVerificationRequest request2 = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Is the Earth round?")
                .build();
        ClaimVerificationResponse response2 = factCheckEngineService.verifyClaim(request2);
        assertEquals("NON-VERIFIABLE INPUT", response2.getVerdict());
        assertEquals("BLOCKED", response2.getPipelineStatus());
        assertNull(response2.getGenuinenessScore());
    }

    @Test
    @DisplayName("TEST 03 / TEST D: 'I think this news is terrible.' MUST classify as OPINION and block verification")
    public void testRegression03OpinionDetection() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("I think this news is terrible.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertEquals("BLOCKED", response.getPipelineStatus());
        assertTrue(response.getInputType().contains("OPINION"));
        assertNull(response.getGenuinenessScore());
        assertNull(response.getSupportScore());
    }

    @Test
    @DisplayName("TEST 04 / TEST C / TEST E: Declarative claims MUST allow verification")
    public void testRegression04FactualClaimsAllowed() {
        ClaimVerificationRequest request1 = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("The Earth is round.")
                .build();
        ClaimVerificationResponse response1 = factCheckEngineService.verifyClaim(request1);
        assertEquals("COMPLETED", response1.getPipelineStatus());
        assertTrue(response1.getClaimDetected());

        ClaimVerificationRequest request2 = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("The government announced a new policy on Monday.")
                .build();
        ClaimVerificationResponse response2 = factCheckEngineService.verifyClaim(request2);
        assertEquals("COMPLETED", response2.getPipelineStatus());
        assertTrue(response2.getClaimDetected());
    }

    @Test
    @DisplayName("TEST 06: Evidence Relevance Admission Gate must reject semantically mismatched articles")
    public void testRegression06EvidenceRelevanceGate() {
        ExternalFactCheckService service = new ExternalFactCheckService();
        ClaimVerificationResponse.ClaimContextInfo context = ClaimVerificationResponse.ClaimContextInfo.builder()
                .geographicEntities(List.of("North Carolina", "Tennessee"))
                .domain("Disaster Relief")
                .build();

        // Irrelevant lifestyle book review with high lexical overlap on generic words
        double relevance = service.calculateClaimRelevance(
                "Flooding disaster in North Carolina and Tennessee killed residents",
                "What Should My Children Do? by Daniel Susskind - A Book Review in London",
                context
        );

        assertTrue(relevance < 0.35, "Irrelevant article must be rejected by relevance gate, got: " + relevance);
    }

    // ==============================================================================================
    // SPECIFICATION REGRESSION TEST SUITE (Section 46: TEST 01 to TEST 12)
    // ==============================================================================================

    @Test
    @DisplayName("TEST 01: 'what should i do' -> NON-VERIFIABLE INPUT, Score=N/A, 0 search, no fabricated claim")
    public void testSpecTest01_WhatShouldIDo() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("what should i do")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertNull(response.getGenuinenessScore(), "Score must be null (rendered as N/A)");
        assertNull(response.getSupportScore(), "Support score must be null (rendered as N/A)");
        assertEquals("BLOCKED", response.getPipelineStatus());
        assertFalse(response.getClaimDetected());
        assertFalse(response.getVerifiable());
        assertTrue(response.getSources().isEmpty(), "Evidence sources must be empty (0 searches)");
        assertEquals("NOT_EXECUTED", response.getPipelineSteps().get(4).getStatus()); // Stage 05 Retrieval
    }

    @Test
    @DisplayName("TEST 02: 'fake news' -> NON-VERIFIABLE INPUT, Score=N/A, 0 evidence")
    public void testSpecTest02_FakeNewsFragment() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("fake news")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertNull(response.getGenuinenessScore(), "Score must be null (rendered as N/A)");
        assertNull(response.getSupportScore(), "Support score must be null (rendered as N/A)");
        assertEquals("BLOCKED", response.getPipelineStatus());
        assertFalse(response.getClaimDetected());
        assertTrue(response.getSources().isEmpty());
    }

    @Test
    @DisplayName("TEST 03: 'Is this news true?' -> NON-VERIFIABLE INPUT, Score=N/A")
    public void testSpecTest03_IsThisNewsTrue() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Is this news true?")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertNull(response.getGenuinenessScore(), "Score must be null (N/A)");
        assertEquals("BLOCKED", response.getPipelineStatus());
    }

    @Test
    @DisplayName("TEST 04: 'India dispatched relief materials to Nepal.' -> VERIFIABLE_CLAIM, claimDetected=true")
    public void testSpecTest04_IndiaReliefNepal() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("India dispatched relief materials to Nepal.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getPipelineStatus());
        assertTrue(response.getClaimDetected());
        assertTrue(response.getVerifiable());
        assertNotNull(response.getClaimType());
        assertNotNull(response.getClaimFingerprint());
        assertEquals("Nepal", response.getClaimFingerprint().getLocation());
    }

    @Test
    @DisplayName("TEST 05: 'Floods killed exactly 500 people.' vs 95 official -> NUMERICAL_CONTRADICTION")
    public void testSpecTest05_FloodsKilledExactly500() {
        ExternalFactCheckService service = new ExternalFactCheckService();
        ExternalFactCheckService.ContradictionCheck check = service.detectContradiction(
                "Floods killed exactly 500 people in Nepal",
                "Nepal flood disaster death toll reaches 95 as rescue continues"
        );

        assertTrue(check.isContradicted(), "Exact numerical mismatch must trigger contradiction");
        assertEquals("MAJOR_CONTRADICTION", check.getSeverity());
        assertEquals("NUMERICAL_DISTORTION", check.getDistortionType());
    }

    @Test
    @DisplayName("TEST 06: 'Floods killed at least 95 people.' vs 102 official -> COMPATIBLE (No contradiction)")
    public void testSpecTest06_FloodsKilledAtLeast95() {
        ExternalFactCheckService service = new ExternalFactCheckService();
        ExternalFactCheckService.ContradictionCheck check = service.detectContradiction(
                "Floods killed at least 95 people in Nepal",
                "Nepal flood disaster death toll rises to 102"
        );

        assertFalse(check.isContradicted(), "'at least 95' vs 102 must be COMPATIBLE");
    }

    @Test
    @DisplayName("TEST 07: Image with readable news claim -> OCR claim confirmation -> verification")
    public void testSpecTest07_ReadableNewsImage() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("IMAGE")
                .content("data:image/jpeg;base64,/9j/4AAQSkZJRg==")
                .title("NASA discovers water vapor on exoplanet LHS 1140b")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getPipelineStatus());
        assertTrue(response.getClaimDetected());
        assertNotNull(response.getGenuinenessScore());
    }

    @Test
    @DisplayName("TEST 08: Pure photograph without textual claim -> NO_VERIFIABLE_CLAIM, Score=N/A")
    public void testSpecTest08_PurePhotograph() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("IMAGE")
                .content("")
                .title("")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE IMAGE", response.getVerdict());
        assertNull(response.getGenuinenessScore());
        assertNull(response.getSupportScore());
    }

    @Test
    @DisplayName("TEST 09: Unreadable screenshot -> OCR_UNRELIABLE, Score=N/A, Verification Blocked")
    public void testSpecTest09_UnreadableScreenshot() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("x_#%&__~~~~~~[[[[[]]]")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertEquals("BLOCKED", response.getPipelineStatus());
        assertNull(response.getGenuinenessScore());
    }

    @Test
    @DisplayName("TEST 10: Valid claim + unrelated Guardian article -> NOT_RELEVANT, contribution=0")
    public void testSpecTest10_UnrelatedGuardianArticleRelevance() {
        ExternalFactCheckService service = new ExternalFactCheckService();
        ClaimVerificationResponse.ClaimContextInfo context = ClaimVerificationResponse.ClaimContextInfo.builder()
                .geographicEntities(List.of("Nepal"))
                .domain("Disaster Relief")
                .build();

        double relevance = service.calculateClaimRelevance(
                "Floods killed 95 people in Nepal",
                "What Should My Children Do? by Daniel Susskind",
                context
        );

        assertTrue(relevance < 0.35, "Unrelated Guardian article must have relevance < 0.35, was: " + relevance);
    }

    @Test
    @DisplayName("TEST 11: Valid claim + 5 copies of same PTI report -> 1 Independent Cluster")
    public void testSpecTest11_SyndicatedArticlesCluster() {
        ExternalFactCheckService service = new ExternalFactCheckService();
        List<ClaimVerificationResponse.SourceEvidence> sources = List.of(
                ClaimVerificationResponse.SourceEvidence.builder().sourceName("Press Trust of India").clusterId("CLUSTER-PTI-01").build(),
                ClaimVerificationResponse.SourceEvidence.builder().sourceName("NDTV (via PTI)").clusterId("CLUSTER-PTI-01").build(),
                ClaimVerificationResponse.SourceEvidence.builder().sourceName("The Hindu (via PTI)").clusterId("CLUSTER-PTI-01").build(),
                ClaimVerificationResponse.SourceEvidence.builder().sourceName("Indian Express (via PTI)").clusterId("CLUSTER-PTI-01").build(),
                ClaimVerificationResponse.SourceEvidence.builder().sourceName("Deccan Herald (via PTI)").clusterId("CLUSTER-PTI-01").build()
        );

        // All 5 sources share clusterId "CLUSTER-PTI-01" -> 1 independent cluster
        long distinctClusters = sources.stream().map(ClaimVerificationResponse.SourceEvidence::getClusterId).distinct().count();
        assertEquals(1, distinctClusters, "5 syndicated PTI copies must collapse into 1 independent cluster");
    }

    @Test
    @DisplayName("TEST 12: Valid claim + PTI + independent official source -> 2 Independent Clusters")
    public void testSpecTest12_IndependentSourcesClusters() {
        List<ClaimVerificationResponse.SourceEvidence> sources = List.of(
                ClaimVerificationResponse.SourceEvidence.builder().sourceName("Press Trust of India").clusterId("C001").isPrimarySource(false).build(),
                ClaimVerificationResponse.SourceEvidence.builder().sourceName("Nepal Police Official Registry").clusterId("C002").isPrimarySource(true).build()
        );

        long distinctClusters = sources.stream().map(ClaimVerificationResponse.SourceEvidence::getClusterId).distinct().count();
        assertEquals(2, distinctClusters, "PTI + Official Police Source must form 2 independent clusters");
    }
}
