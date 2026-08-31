package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimVerificationRequest;
import com.truthlens.api.dto.ClaimVerificationResponse;
import com.truthlens.api.nlp.*;
import com.truthlens.api.repository.VerifiedSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    @DisplayName("Question with embedded factual proposition should extract claim and verify")
    public void testFactualQuestionExtraction() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Did India send relief materials to Nepal?")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertFalse(response.getVerdict().contains("NON-VERIFIABLE"), "Factual question should extract proposition and verify!");
        assertNotNull(response.getClaimContext());
        assertTrue(response.getClaimContext().getGeographicEntities().contains("Nepal") || response.getClaimContext().getGeographicEntities().contains("India"));
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
    @DisplayName("Unverified Arbitrary Rumor should NOT score as Genuine (Score <= 50)")
    public void testUnverifiedArbitraryClaim() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("A local mayor was spotted secretly purchasing a naval submarine in international waters yesterday.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getVerdict().contains("INSUFFICIENT") || 
                   response.getVerdict().contains("MIXED") || 
                   response.getVerdict().contains("CONFLICTING") ||
                   response.getVerdict().contains("PARTIALLY"));
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
        assertTrue(response.getVerdict().contains("INSUFFICIENT") || response.getVerdict().contains("NON-VERIFIABLE") || response.getVerdict().contains("CONTRADICTED"));
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
    @DisplayName("Unverified rumor about PM Modi passing should NOT match Queen Elizabeth and should score <= 50")
    public void testUnverifiedModiDeathRumor() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Indian PM Narendra Modi Passed away")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 50, "Score must not be genuine, was: " + response.getGenuinenessScore());
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
        assertFalse(response.getVerdict().contains("NON-VERIFIABLE") && response.getGenuinenessScore() == null);
        assertNotNull(response.getGenuinenessScore(), "Score should be calculated!");
    }

    @Test
    @DisplayName("Valid URL input should NOT be rejected as input too short")
    public void testUrlInputVerifiability() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("URL")
                .content("https://www.reuters.com/world/science/nasa-james-webb-discovery")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertFalse(response.getVerdict().contains("NON-VERIFIABLE") && response.getGenuinenessScore() == null);
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
    @DisplayName("Pure photograph with no extractable text should return NON-VERIFIABLE INPUT with NO_CLAIM_FOUND")
    public void testPurePhotographNoClaim() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("IMAGE")
                .content("")
                .title("")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertNull(response.getSupportScore());
        assertNotNull(response.getImageAnalysis());
        assertEquals("NO_CLAIM_FOUND", response.getImageAnalysis().getClaimExtractionStatus());
        assertTrue(response.getImageAnalysis().isRequiresUserReview());
    }
}

