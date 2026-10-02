package com.truthlens.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.api.dto.ClaimVerificationRequest;
import com.truthlens.api.dto.ClaimVerificationResponse;
import com.truthlens.api.nlp.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TruthLensBenchmarkEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(TruthLensBenchmarkEvaluationTest.class);

    private FactCheckEngineService engineService;
    private ClaimVerifiabilityValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TfIdfVectoriser tfIdfVectoriser = new TfIdfVectoriser();
        CosineSimilarityEngine cosineSimilarityEngine = new CosineSimilarityEngine();
        FactCheckingCorpus corpus = new FactCheckingCorpus(tfIdfVectoriser, cosineSimilarityEngine);
        corpus.init();

        NamedEntityExtractor namedEntityExtractor = new NamedEntityExtractor();
        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
        ClickbaitClassifier clickbaitClassifier = new ClickbaitClassifier();
        NlpPipelineService nlpService = new NlpPipelineService(namedEntityExtractor, sentimentAnalyzer, clickbaitClassifier);

        OcrAnalysisService ocrService = new OcrAnalysisService();
        ClaimContextService contextService = new ClaimContextService();
        ExternalFactCheckService externalService = new ExternalFactCheckService(contextService);
        validator = new ClaimVerifiabilityValidator();
        ClaimDecompositionService decompService = new ClaimDecompositionService();
        ClaimRelationAnalyzer relationAnalyzer = new ClaimRelationAnalyzer();

        engineService = new FactCheckEngineService(
                nlpService,
                ocrService,
                corpus,
                externalService,
                null,
                validator,
                decompService,
                relationAnalyzer,
                contextService,
                null,
                null
        );

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Evaluate TruthLens against Labeled Benchmark Dataset (v1.0)")
    void testBenchmarkEvaluationSuite() throws Exception {
        InputStream is = getClass().getResourceAsStream("/benchmark/truthlens_benchmark_v1.json");
        assertNotNull(is, "Benchmark dataset file truthlens_benchmark_v1.json must exist in classpath");

        List<Map<String, Object>> benchmarkCases = objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
        assertFalse(benchmarkCases.isEmpty(), "Benchmark dataset must contain test items");

        int totalCases = benchmarkCases.size();
        int passedGateCount = 0;
        int correctlyClassifiedCount = 0;
        int nonVerifiablePassedGate = 0;
        int nonVerifiableTotal = 0;

        log.info("=== STARTING TRUTHLENS BENCHMARK EVALUATION ({} Test Cases) ===", totalCases);

        for (Map<String, Object> tc : benchmarkCases) {
            String id = (String) tc.get("id");
            String input = (String) tc.get("input");
            boolean expectedVerifiable = Boolean.TRUE.equals(tc.get("expectedVerifiable"));
            String expectedVerdictCat = (String) tc.get("expectedVerdictCategory");
            String desc = (String) tc.get("description");

            ClaimVerificationRequest req = ClaimVerificationRequest.builder()
                    .content(input)
                    .type("TEXT")
                    .build();

            ClaimVerificationResponse resp = engineService.verifyClaim(req);

            assertNotNull(resp, "Response must not be null for TC: " + id);

            if (!expectedVerifiable) {
                nonVerifiableTotal++;
                // Non-verifiable inputs MUST receive null scores and non-verifiable verdicts
                boolean isBlockedOrInsufficient = !resp.getVerifiable() || resp.getGenuinenessScore() == null;
                if (isBlockedOrInsufficient) {
                    nonVerifiablePassedGate++;
                    correctlyClassifiedCount++;
                } else {
                    log.error("FAIL: [TC {}] Input '{}' should be blocked but got score {}", id, input, resp.getGenuinenessScore());
                }
                assertNull(resp.getGenuinenessScore(), "Score MUST be null for non-verifiable query: " + input);
            } else {
                // Verifiable inputs
                if (resp.getVerifiable()) {
                    passedGateCount++;
                    correctlyClassifiedCount++;
                }

                if ("VERIFIED".equals(expectedVerdictCat)) {
                    assertTrue(resp.getVerdict().contains("VERIFIED") || resp.getVerdict().contains("SUPPORTED"),
                            "TC " + id + " expected verified/supported verdict but got: " + resp.getVerdict());
                } else if ("CONTRADICTED".equals(expectedVerdictCat)) {
                    assertTrue(resp.getVerdict().contains("CONTRADICTED") || resp.getVerdict().contains("FABRICATED") || resp.getVerdict().contains("HOAX"),
                            "TC " + id + " expected contradicted/hoax verdict but got: " + resp.getVerdict());
                }
            }

            log.info("[TC {} - {}] Input: '{}' -> Verdict: '{}' (Score: {}, Base: {}, Penalty: {})",
                    id, desc, input, resp.getVerdict(), resp.getGenuinenessScore(), resp.getBaseSupportScore(), resp.getContradictionPenalty());
        }

        double gateAccuracy = ((double) nonVerifiablePassedGate / nonVerifiableTotal) * 100.0;
        double overallAccuracy = ((double) correctlyClassifiedCount / totalCases) * 100.0;

        log.info("=== BENCHMARK EVALUATION RESULTS ===");
        log.info("Total Cases Evaluated: {}", totalCases);
        log.info("Anti-Hallucination Gate Accuracy on Non-Verifiable inputs: {}% ({}/{})", gateAccuracy, nonVerifiablePassedGate, nonVerifiableTotal);
        log.info("Overall Benchmark Accuracy: {}% ({}/{})", overallAccuracy, correctlyClassifiedCount, totalCases);

        assertEquals(100.0, gateAccuracy, "Anti-Hallucination Gate MUST achieve 100% accuracy on blocking non-claims!");
        assertTrue(overallAccuracy >= 90.0, "Overall Benchmark Accuracy should exceed 90%");
    }

    @Test
    @DisplayName("Verify Explicit Deterministic Base Score and Scaled Penalty Explainability Profile")
    void testExplainabilityFormulas() {
        ClaimVerificationRequest req = ClaimVerificationRequest.builder()
                .content("India dispatched humanitarian aid and disaster relief materials to Nepal following flash floods.")
                .type("TEXT")
                .build();

        ClaimVerificationResponse resp = engineService.verifyClaim(req);
        assertNotNull(resp.getExplainability());
        assertNotNull(resp.getExplainability().getBaseScoreFormula(), "baseScoreFormula should be present");
        assertNotNull(resp.getExplainability().getPenaltyScalingFormula(), "penaltyScalingFormula should be present");
        assertNotNull(resp.getExplainability().getPerClusterContributions(), "perClusterContributions should be present");
        assertTrue(resp.getExplainability().getBaseScoreFormula().contains("min(100"));
    }

    @Test
    @DisplayName("Verify Compound Conversational Question Extraction Gate")
    void testCompoundQuestionExtraction() {
        ClaimVerificationRequest req = ClaimVerificationRequest.builder()
                .content("Is it true that India dispatched relief materials to Nepal?")
                .type("TEXT")
                .build();

        ClaimVerificationResponse resp = engineService.verifyClaim(req);
        assertTrue(resp.getVerifiable(), "Compound question with declarative proposition should pass verifiability gate");
        assertNotNull(resp.getGenuinenessScore(), "Score should be computed for extracted proposition");
    }

    @Test
    @DisplayName("Verify Strict Rejection of Non-Claim Query 'what should i do'")
    void testStrictRejectionOfQuestion() {
        ClaimVerificationRequest req = ClaimVerificationRequest.builder()
                .content("what should i do")
                .type("TEXT")
                .build();

        ClaimVerificationResponse resp = engineService.verifyClaim(req);
        assertFalse(resp.getVerifiable(), "Non-claim question MUST be blocked");
        assertNull(resp.getGenuinenessScore(), "Score MUST be null (N/A)");
        assertEquals("NON-VERIFIABLE INPUT", resp.getVerdict());
    }
}
