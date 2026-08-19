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
    @DisplayName("Interrogative Question Input should return NON-VERIFIABLE INPUT")
    public void testQuestionInput() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("what is this?")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertEquals(0, response.getGenuinenessScore());
        assertTrue(response.getRationale().toLowerCase().contains("does not constitute"));
    }

    @Test
    @DisplayName("Single Word Input should return NON-VERIFIABLE INPUT")
    public void testSingleWordInput() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("apple")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertEquals(0, response.getGenuinenessScore());
    }

    @Test
    @DisplayName("Conversational Greeting should return NON-VERIFIABLE INPUT")
    public void testGreetingInput() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("hello how are you")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertEquals(0, response.getGenuinenessScore());
    }

    @Test
    @DisplayName("Debunked Health Fake News should return FABRICATED / FAKE with score <= 25")
    public void testCancerCureFakeNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Drinking boiled lemon water and baking soda cures cancer in 48 hours without chemotherapy!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertEquals("FABRICATED / FAKE", response.getVerdict());
        assertFalse(response.getSources().isEmpty());
        assertTrue(response.getRationale().toLowerCase().contains("cancer") || response.getRationale().toLowerCase().contains("debunked") || response.getRationale().toLowerCase().contains("chemotherapy"));
    }

    @Test
    @DisplayName("5G Conspiracy Fake News should return FABRICATED / FAKE with score <= 25")
    public void test5gConspiracyFakeNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("5G wireless towers are emitting radiation that creates covid viruses to control human brains!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertEquals("FABRICATED / FAKE", response.getVerdict());
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
        assertTrue(response.getGenuinenessScore() <= 50, "Unverified claim should NOT default to genuine, score was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().equals("MIXED / UNVERIFIED") || response.getVerdict().equals("LIKELY MISLEADING"));
    }

    @Test
    @DisplayName("Verified NASA Discovery should score >= 80 and return VERIFIED GENUINE / MOSTLY GENUINE")
    public void testVerifiedNasaNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("NASA James Webb Space Telescope discovers atmospheric water vapor on distant exoplanet LHS 1140b.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE"));
    }

    @Test
    @DisplayName("Verified WHO Guidelines should score >= 80 and return VERIFIED GENUINE / MOSTLY GENUINE")
    public void testVerifiedWhoNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("World Health Organization publishes updated clinical guidelines on seasonal viral mitigation and vaccination schedules.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE"));
    }

    @Test
    @DisplayName("Debunked Flat Earth Hoax should return FABRICATED / FAKE with score <= 25")
    public void testFlatEarthHoax() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("The Earth is flat and the Antarctic ice wall is guarded by United Nations warships to hide the truth!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertEquals("FABRICATED / FAKE", response.getVerdict());
    }

    @Test
    @DisplayName("Debunked Vaccine Microchip Conspiracy should return FABRICATED / FAKE with score <= 25")
    public void testVaccineMicrochipConspiracy() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("COVID-19 vaccines contain microscopic tracking microchips and nanobots for government population surveillance!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertEquals("FABRICATED / FAKE", response.getVerdict());
    }

    @Test
    @DisplayName("Debunked World Bank Debt Forgiveness Scam should return FABRICATED / FAKE with score <= 25")
    public void testWorldBankDebtScam() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("World Bank announced all national personal debts and mortgages will be wiped clean by Friday!")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() <= 25, "Score should be <= 25, but was: " + response.getGenuinenessScore());
        assertEquals("FABRICATED / FAKE", response.getVerdict());
    }

    @Test
    @DisplayName("Verified CRISPR Gene Therapy should score >= 80 and return VERIFIED GENUINE / MOSTLY GENUINE")
    public void testVerifiedCrisprNews() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("FDA approves breakthrough CRISPR Cas9 gene editing therapeutic Casgevy for treatment of sickle cell disease.")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE"));
    }

    @Test
    @DisplayName("Question 'Did the Prime Minister pass away?' should return NON-VERIFIABLE INPUT")
    public void testDidPrimeMinisterPassAwayQuestion() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Did the Prime Minister pass away?")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertEquals("NON-VERIFIABLE INPUT", response.getVerdict());
        assertEquals(0, response.getGenuinenessScore());
    }

    @Test
    @DisplayName("Verified Queen Elizabeth II passing should return VERIFIED GENUINE with score >= 80")
    public void testQueenElizabethPassingVerified() {
        ClaimVerificationRequest request = ClaimVerificationRequest.builder()
                .type("TEXT")
                .content("Queen Elizabeth II passed away at Balmoral on September 8, 2022")
                .build();

        ClaimVerificationResponse response = factCheckEngineService.verifyClaim(request);

        assertNotNull(response);
        assertTrue(response.getGenuinenessScore() >= 80, "Score should be >= 80, but was: " + response.getGenuinenessScore());
        assertTrue(response.getVerdict().contains("GENUINE"));
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
        assertFalse(response.getVerdict().contains("GENUINE"));
        assertFalse(response.getRationale().toLowerCase().contains("queen elizabeth"), "Rationale must NOT contain Queen Elizabeth!");
    }
}
