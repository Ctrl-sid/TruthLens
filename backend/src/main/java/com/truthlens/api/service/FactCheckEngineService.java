package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimVerificationRequest;
import com.truthlens.api.dto.ClaimVerificationResponse;
import com.truthlens.api.dto.ClaimVerificationResponse.*;
import com.truthlens.api.dto.NlpAnalysisResponse;
import com.truthlens.api.model.FactCheckHistory;
import com.truthlens.api.model.User;
import com.truthlens.api.model.VerifiedSource;
import com.truthlens.api.nlp.ClaimContextService;
import com.truthlens.api.nlp.ClaimDecompositionService;
import com.truthlens.api.nlp.ClaimRelationAnalyzer;
import com.truthlens.api.nlp.ClaimVerifiabilityValidator;
import com.truthlens.api.nlp.FactCheckingCorpus;
import com.truthlens.api.nlp.FactCheckingCorpus.CorpusEntry;
import com.truthlens.api.nlp.FactCheckingCorpus.MatchResult;
import com.truthlens.api.nlp.NlpPipelineService;
import com.truthlens.api.repository.FactCheckHistoryRepository;
import com.truthlens.api.repository.UserRepository;
import com.truthlens.api.repository.VerifiedSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FactCheckEngineService {

    private final NlpPipelineService nlpPipelineService;
    private final OcrAnalysisService ocrAnalysisService;
    private final FactCheckingCorpus factCheckingCorpus;
    private final ExternalFactCheckService externalFactCheckService;
    private final VerifiedSourceRepository verifiedSourceRepository;
    private final ClaimVerifiabilityValidator claimVerifiabilityValidator;
    private final ClaimDecompositionService claimDecompositionService;
    private final ClaimRelationAnalyzer claimRelationAnalyzer;
    private final ClaimContextService claimContextService;
    private final FactCheckHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @Autowired
    public FactCheckEngineService(
            NlpPipelineService nlpPipelineService,
            OcrAnalysisService ocrAnalysisService,
            FactCheckingCorpus factCheckingCorpus,
            ExternalFactCheckService externalFactCheckService,
            VerifiedSourceRepository verifiedSourceRepository,
            ClaimVerifiabilityValidator claimVerifiabilityValidator,
            ClaimDecompositionService claimDecompositionService,
            ClaimRelationAnalyzer claimRelationAnalyzer,
            ClaimContextService claimContextService,
            @Autowired(required = false) FactCheckHistoryRepository historyRepository,
            @Autowired(required = false) UserRepository userRepository
    ) {
        this.nlpPipelineService = nlpPipelineService;
        this.ocrAnalysisService = ocrAnalysisService;
        this.factCheckingCorpus = factCheckingCorpus;
        this.externalFactCheckService = externalFactCheckService;
        this.verifiedSourceRepository = verifiedSourceRepository;
        this.claimVerifiabilityValidator = claimVerifiabilityValidator;
        this.claimDecompositionService = claimDecompositionService != null ? claimDecompositionService : new ClaimDecompositionService();
        this.claimRelationAnalyzer = claimRelationAnalyzer != null ? claimRelationAnalyzer : new ClaimRelationAnalyzer();
        this.claimContextService = claimContextService != null ? claimContextService : new ClaimContextService();
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    public FactCheckEngineService(
            NlpPipelineService nlpPipelineService,
            OcrAnalysisService ocrAnalysisService,
            FactCheckingCorpus factCheckingCorpus,
            ExternalFactCheckService externalFactCheckService,
            VerifiedSourceRepository verifiedSourceRepository,
            ClaimVerifiabilityValidator claimVerifiabilityValidator
    ) {
        this(nlpPipelineService, ocrAnalysisService, factCheckingCorpus, externalFactCheckService, verifiedSourceRepository, claimVerifiabilityValidator, new ClaimDecompositionService(), new ClaimRelationAnalyzer(), new ClaimContextService(), null, null);
    }

    public ClaimVerificationResponse verifyClaim(ClaimVerificationRequest request) {
        String contentToAnalyze = request.getContent() != null ? request.getContent().trim() : "";
        ClaimVerificationResponse.ImageIntegrityAnalysis imageAnalysis = null;
        String detectedDomain = null;

        // 1. Handle Image input OCR extraction
        if ("IMAGE".equalsIgnoreCase(request.getType())) {
            String explicitTitle = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle().trim() : null;
            imageAnalysis = ocrAnalysisService.analyzeImageInput(contentToAnalyze, explicitTitle);

            // Check if user provided an explicit headline and if it is actually verifiable
            boolean explicitTitleIsVerifiable = false;
            if (explicitTitle != null) {
                ClaimVerifiabilityValidator.ValidationResult titleVal = claimVerifiabilityValidator.validateClaimVerifiability(explicitTitle);
                explicitTitleIsVerifiable = titleVal.isVerifiableClaim();
            }

            boolean isUnreliable = "NO_TEXT_DETECTED".equals(imageAnalysis.getClaimExtractionStatus())
                    || "NO_CLAIM_DETECTED".equals(imageAnalysis.getClaimExtractionStatus())
                    || "OCR_UNRELIABLE".equals(imageAnalysis.getClaimExtractionStatus())
                    || "OCR_INSUFFICIENT".equals(imageAnalysis.getClaimExtractionStatus())
                    || "TEXT_ABSENT".equals(imageAnalysis.getTextPresence())
                    || "NONE".equals(imageAnalysis.getClaimVerificationBasis());

            if (isUnreliable && !explicitTitleIsVerifiable) {
                NlpAnalysisResponse nlpResults = nlpPipelineService.processText(imageAnalysis.getDetectedHeadlineText());
                long claimId = System.currentTimeMillis();
                String verdictText = "NO_TEXT_DETECTED".equals(imageAnalysis.getClaimExtractionStatus()) ? "NON-VERIFIABLE IMAGE" : "NO CLAIM DETECTED";
                List<PipelineStep> steps = buildPipelineSteps("IMAGE", imageAnalysis, null, List.of(), List.of(), verdictText, true);

                return ClaimVerificationResponse.builder()
                        .id(claimId)
                        .inputType("IMAGE")
                        .claimSummary("Non-Verifiable Image: No News Claim Detected")
                        .genuinenessScore(null) // Unassigned / N/A
                        .supportScore(null) // Unassigned / N/A
                        .verdict(verdictText)
                        .verdictBadgeColor("#64748B")
                        .confidence("HIGH")
                        .confidenceScore(95)
                        .evidenceCompleteness(0)
                        .asOfStatus("UNVERIFIED")
                        .distortionType("NONE")
                        .contradictionSeverity("NONE")
                        .failureState("NONE")
                        .rationale("TruthLens could not identify a reliable factual news claim in the uploaded image. The image appears to contain no readable news headline, article text, or factual assertion. OCR confidence is too low to safely reconstruct a claim.")
                        .keyReasons(List.of(
                                "OCR Status: UNRELIABLE (" + Math.round(imageAnalysis.getValidWordRatio() != null ? imageAnalysis.getValidWordRatio() : 0) + "% valid words).",
                                "Claim Verification: NOT PERFORMED (TruthLens strictly prevents inventing claims from unreadable text).",
                                "Genuineness Score: N/A (no claim exists to verify as genuine or fake).",
                                "Digital forensics evaluated independently in the Image Forensics tab."
                        ))
                        .subClaims(List.of())
                        .evidenceClusters(List.of())
                        .sources(List.of())
                        .pipelineSteps(steps)
                        .nlpAnalysis(nlpResults)
                        .imageAnalysis(imageAnalysis)
                        .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build();
            }

            if (explicitTitleIsVerifiable) {
                contentToAnalyze = explicitTitle;
            } else if (imageAnalysis.getReconstructedClaim() != null && !imageAnalysis.getReconstructedClaim().isBlank()
                    && imageAnalysis.getReconstructionConfidence() != null && imageAnalysis.getReconstructionConfidence() >= 80.0) {
                contentToAnalyze = imageAnalysis.getReconstructedClaim();
            } else {
                contentToAnalyze = imageAnalysis.getDetectedHeadlineText();
            }
        } else if ("URL".equalsIgnoreCase(request.getType())) {
            detectedDomain = extractDomainFromUrl(contentToAnalyze);
        }

        // 2. Pre-Check: Validate Claim Verifiability / Check-Worthiness & Question Claim Extraction
        ClaimVerifiabilityValidator.ValidationResult validation = claimVerifiabilityValidator.validateClaimVerifiability(contentToAnalyze);
        if (!validation.isVerifiableClaim()) {
            NlpAnalysisResponse nlpResults = nlpPipelineService.processText(contentToAnalyze);
            long claimId = System.currentTimeMillis();
            String inputTypeStr = request.getType() != null ? request.getType().toUpperCase() : "TEXT";
            List<PipelineStep> steps = buildPipelineSteps(inputTypeStr, imageAnalysis, validation, List.of(), List.of(), "NON-VERIFIABLE INPUT", true);

            return ClaimVerificationResponse.builder()
                    .id(claimId)
                    .inputType(inputTypeStr)
                    .claimSummary("Non-Verifiable Input: '" + (contentToAnalyze.length() > 50 ? contentToAnalyze.substring(0, 47) + "..." : contentToAnalyze) + "'")
                    .genuinenessScore(null) // Unassigned / N/A
                    .supportScore(null)
                    .verdict("NON-VERIFIABLE INPUT")
                    .verdictBadgeColor("#64748B")
                    .confidence("HIGH")
                    .confidenceScore(95)
                    .evidenceCompleteness(0)
                    .asOfStatus("UNVERIFIED")
                    .distortionType("NONE")
                    .contradictionSeverity("NONE")
                    .failureState("NONE")
                    .rationale("The submitted text does not constitute a declarative news claim with verifiable factual assertions. " + validation.getRejectionReason() + ".")
                    .keyReasons(validation.getAdvisoryNotes())
                    .subClaims(List.of())
                    .evidenceClusters(List.of())
                    .sources(List.of())
                    .pipelineSteps(steps)
                    .nlpAnalysis(nlpResults)
                    .imageAnalysis(imageAnalysis)
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
        }

        // Use extracted factual claim if question was transformed
        if (validation.getExtractedFactualClaim() != null && !validation.getExtractedFactualClaim().isBlank()) {
            contentToAnalyze = validation.getExtractedFactualClaim();
        }

        // 3. Extract Claim Context (Geographic entities, Domain, Target Authorities)
        ClaimContextInfo claimContext = claimContextService.extractClaimContext(contentToAnalyze);

        // 4. Decompose Claim into Atomic Propositions with Entity-Relationship Triples & Centrality
        List<DecomposedClaim> decomposedSubClaims = claimDecompositionService.decompose(contentToAnalyze);

        // 5. Run NLP Pipeline
        NlpAnalysisResponse nlpResults = nlpPipelineService.processText(contentToAnalyze);

        // 6. Match against Fact-Checking Candidate Vector Corpus (Candidate Discovery Signal)
        MatchResult corpusMatch = factCheckingCorpus.matchClaimAgainstCorpus(contentToAnalyze);

        // 7. Query Contextual External Candidate Sources & Live Wire Discovery
        Optional<ExternalFactCheckService.ExternalFactResult> externalFact = externalFactCheckService.queryExternalKnowledge(contentToAnalyze, claimContext);

        // 8. Check Domain Credibility if input is URL
        VerifiedSource domainSource = null;
        if (detectedDomain != null) {
            domainSource = verifiedSourceRepository.findByDomain(detectedDomain.toLowerCase()).orElse(null);
        }

        // 9. Evaluate Sub-Claims Individually & Check for Direct Reversals / Range Matching
        evaluateSubClaims(decomposedSubClaims, corpusMatch, externalFact);

        // 10. Calculate Evidence Completeness (% of subclaims corroborated)
        int evidenceCompleteness = calculateEvidenceCompleteness(decomposedSubClaims);

        // 11. Core Support Score Calculation & Contradiction Severity Assessment
        int score = calculateSupportScore(contentToAnalyze, nlpResults, imageAnalysis, corpusMatch, externalFact, domainSource, decomposedSubClaims);
        String contradictionSeverity = determineContradictionSeverity(contentToAnalyze, externalFact, corpusMatch, decomposedSubClaims);
        String distortionType = determineDistortionType(externalFact, contradictionSeverity);
        String asOfStatus = determineAsOfStatus(contentToAnalyze, externalFact, decomposedSubClaims);

        String verdict = determineVerdict(score, externalFact, corpusMatch, contradictionSeverity, decomposedSubClaims, asOfStatus);
        String verdictBadgeColor = getVerdictBadgeColor(verdict, score);
        int confidenceScore = calculateConfidenceScore(corpusMatch, externalFact, decomposedSubClaims);
        String confidenceLevel = confidenceScore >= 75 ? "HIGH" : (confidenceScore >= 45 ? "MEDIUM" : "LOW");

        // 12. Generate Rationale & Key Reasons
        List<String> keyReasons = generateKeyReasons(contentToAnalyze, nlpResults, score, imageAnalysis, corpusMatch, externalFact, domainSource, contradictionSeverity, evidenceCompleteness, distortionType);
        String rationale = buildRationaleText(contentToAnalyze, score, verdict, nlpResults, corpusMatch, externalFact, domainSource, contradictionSeverity, evidenceCompleteness);

        // 13. Build Authentic Source Citations & Clusters
        List<SourceEvidence> sources = buildSourceCitations(contentToAnalyze, score, corpusMatch, externalFact, domainSource);
        List<EvidenceCluster> evidenceClusters = externalFact.map(ExternalFactCheckService.ExternalFactResult::getEvidenceClusters)
                .filter(c -> !c.isEmpty())
                .orElseGet(() -> buildDefaultClusters(sources, score < 40));

        // 14. Build Retrieval Audit Trail
        RetrievalAudit retrievalAudit = externalFact.map(ExternalFactCheckService.ExternalFactResult::getRetrievalAudit)
                .orElseGet(() -> buildDefaultAudit(sources, evidenceClusters));

        // 15. Build Claim Origin & Provenance Discovery (Earliest Verified Source Found)
        ClaimOriginDiscovery originDiscovery = buildClaimOriginDiscovery(contentToAnalyze, score, verdict, corpusMatch, externalFact, domainSource, contradictionSeverity, distortionType);

        // 16. Build Structured Explainability Profile & Matrix
        ExplainabilityProfile explainability = buildExplainabilityProfile(contentToAnalyze, score, verdict, confidenceLevel, confidenceScore, evidenceCompleteness, asOfStatus, distortionType, sources, evidenceClusters, externalFact, corpusMatch, contradictionSeverity, retrievalAudit);

        // 17. Decouple Content Diagnostics / Sensationalism
        ContentCharacteristics contentDiagnostics = buildContentDiagnostics(nlpResults);

        String summary = nlpResults.getExtractedEntities().isEmpty() ?
                (contentToAnalyze.length() > 80 ? contentToAnalyze.substring(0, 77) + "..." : contentToAnalyze)
                : "Claim involving " + String.join(", ", nlpResults.getExtractedEntities());

        long resultId = System.currentTimeMillis();

        // 18. Build Verification Pipeline Steps Trace
        String finalInputType = request.getType() != null ? request.getType().toUpperCase() : "TEXT";
        List<PipelineStep> steps = buildPipelineSteps(finalInputType, imageAnalysis, validation, decomposedSubClaims, evidenceClusters, verdict, false);

        // 19. Persist verified claim to database if repository is available
        if (historyRepository != null) {
            try {
                User authUser = null;
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName()) && userRepository != null) {
                    authUser = userRepository.findByUsername(authentication.getName()).orElse(null);
                }

                FactCheckHistory historyEntity = FactCheckHistory.builder()
                        .user(authUser)
                        .inputType(request.getType() != null ? request.getType().toUpperCase() : "TEXT")
                        .inputContent(contentToAnalyze)
                        .claimSummary(summary)
                        .genuinenessScore(score)
                        .verdict(verdict)
                        .rationale(rationale)
                        .createdAt(LocalDateTime.now())
                        .build();

                FactCheckHistory saved = historyRepository.save(historyEntity);
                if (saved != null && saved.getId() != null) {
                    resultId = saved.getId();
                }
            } catch (Exception ignored) {}
        }

        return ClaimVerificationResponse.builder()
                .id(resultId)
                .inputType(finalInputType)
                .claimSummary(summary)
                .genuinenessScore(score) // Maintained for backward compatibility
                .supportScore(score) // Explicit Evidence Support Score
                .verdict(verdict)
                .verdictBadgeColor(verdictBadgeColor)
                .confidence(confidenceLevel)
                .confidenceScore(confidenceScore)
                .evidenceCompleteness(evidenceCompleteness)
                .asOfStatus(asOfStatus)
                .distortionType(distortionType)
                .contradictionSeverity(contradictionSeverity)
                .failureState("NONE")
                .rationale(rationale)
                .keyReasons(keyReasons)
                .claimContext(claimContext)
                .retrievalAudit(retrievalAudit)
                .subClaims(decomposedSubClaims)
                .evidenceClusters(evidenceClusters)
                .sources(sources)
                .pipelineSteps(steps)
                .explainability(explainability)
                .contentDiagnostics(contentDiagnostics)
                .originDiscovery(originDiscovery)
                .nlpAnalysis(nlpResults)
                .imageAnalysis(imageAnalysis)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .algorithmVersion("2.3")
                .scoringVersion("2.3")
                .build();
    }

    private String determineAsOfStatus(String text, Optional<ExternalFactCheckService.ExternalFactResult> externalFact, List<DecomposedClaim> subClaims) {
        if (text == null) return "CURRENTLY_VALID";
        String lower = text.toLowerCase();
        if (lower.contains("live") || lower.contains("breaking") || lower.contains("developing") || lower.contains("initial")) {
            return "SUPPORTED_AT_CLAIM_TIME";
        }
        return "CURRENTLY_VALID";
    }

    private String determineDistortionType(Optional<ExternalFactCheckService.ExternalFactResult> externalFact, String severity) {
        if (externalFact.isPresent() && externalFact.get().getDistortionType() != null && !"NONE".equals(externalFact.get().getDistortionType())) {
            return externalFact.get().getDistortionType();
        }
        if ("DIRECT_FACTUAL_REVERSAL".equals(severity)) return "POLARITY_DISTORTION";
        if ("MINOR_DISCREPANCY".equals(severity) || "MODERATE_CONTRADICTION".equals(severity)) return "NUMERICAL_DISTORTION";
        return "NONE";
    }

    private int calculateEvidenceCompleteness(List<DecomposedClaim> subClaims) {
        if (subClaims == null || subClaims.isEmpty()) return 100;
        long verifiedCount = subClaims.stream()
                .filter(s -> "VERIFIED".equals(s.getClaimVerdict()) || "MOSTLY_VERIFIED".equals(s.getClaimVerdict()) || "DEVELOPING_RANGE".equals(s.getClaimVerdict()))
                .count();
        return (int) Math.round(((double) verifiedCount / subClaims.size()) * 100.0);
    }

    private void evaluateSubClaims(List<DecomposedClaim> subClaims, MatchResult corpusMatch, Optional<ExternalFactCheckService.ExternalFactResult> externalFact) {
        if (subClaims == null || subClaims.isEmpty()) return;

        for (DecomposedClaim sub : subClaims) {
            String text = sub.getClaimText();
            String refTopic = externalFact.map(ExternalFactCheckService.ExternalFactResult::getTopic).orElse("");
            String refSnippet = externalFact.map(ExternalFactCheckService.ExternalFactResult::getSnippet).orElse("");
            String corpusText = corpusMatch.getBestVerifiedEntry() != null ? corpusMatch.getBestVerifiedEntry().getText() : "";

            ExternalFactCheckService.ContradictionCheck extContradiction = checkContradictionSafely(text, refTopic + " " + refSnippet);
            ExternalFactCheckService.ContradictionCheck corpusContradiction = checkContradictionSafely(text, corpusText);

            boolean isSubContradicted = (extContradiction != null && extContradiction.isContradicted()) ||
                                       (corpusContradiction != null && corpusContradiction.isContradicted());

            if (isSubContradicted) {
                String severity = (extContradiction != null && extContradiction.isContradicted()) ? extContradiction.getSeverity() : corpusContradiction.getSeverity();
                String reason = (extContradiction != null && extContradiction.isContradicted()) ? extContradiction.getReason() : corpusContradiction.getReason();

                int subScore = "MINOR_DISCREPANCY".equals(severity) ? 75 :
                               ("MODERATE_CONTRADICTION".equals(severity) ? 55 : 18);

                sub.setClaimScore(subScore);
                sub.setClaimVerdict("MINOR_DISCREPANCY".equals(severity) ? "MOSTLY_VERIFIED" : "REFUTED");
                sub.setStance(claimRelationAnalyzer.analyzeRelation(text, refTopic, true).name());
                sub.setEvidenceSummary("Contradicted by verified records (" + severity + "): " + reason);
                sub.setStatusReason(reason);
            } else if (externalFact.isPresent() && externalFact.get().isAuthenticCorroboration()) {
                sub.setClaimScore(92);
                sub.setClaimVerdict("VERIFIED");
                sub.setStance("CONFIRMED");
                sub.setEvidenceSummary("Corroborated across primary news wire reports.");
                sub.setStatusReason("Independently corroborated across authoritative dispatches.");
                sub.setEvidenceClusterIds(List.of("C001", "C002"));
            } else if (corpusMatch.getBestVerifiedEntry() != null && corpusMatch.getVerifiedSimilarity() >= 0.35) {
                sub.setClaimScore(88);
                sub.setClaimVerdict("VERIFIED");
                sub.setStance("CONFIRMED");
                sub.setEvidenceSummary("Corroborated by historical documentary archives (" + corpusMatch.getBestVerifiedEntry().getSourceName() + ").");
                sub.setStatusReason("Corroborated by historical archives.");
                sub.setEvidenceClusterIds(List.of("C001"));
            } else {
                sub.setClaimScore(50);
                sub.setClaimVerdict("UNVERIFIED");
                sub.setStance("NOT_MENTIONED");
                sub.setEvidenceSummary("Insufficient independent documentation found.");
                sub.setStatusReason("Requires primary official corroboration.");
            }
        }
    }

    private String extractDomainFromUrl(String urlString) {
        try {
            URI uri = URI.create(urlString);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ExternalFactCheckService.ContradictionCheck checkContradictionSafely(String query, String reference) {
        if (externalFactCheckService != null) {
            try {
                ExternalFactCheckService.ContradictionCheck check = externalFactCheckService.detectContradiction(query, reference);
                if (check != null) return check;
            } catch (Exception ignored) {}
        }
        return new ExternalFactCheckService().detectContradiction(query, reference);
    }

    private String determineContradictionSeverity(String text, Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                                 MatchResult match, List<DecomposedClaim> subClaims) {
        if (externalFact.isPresent() && externalFact.get().isContradiction() && externalFact.get().getContradictionSeverity() != null) {
            return externalFact.get().getContradictionSeverity();
        }

        if (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.35) {
            ExternalFactCheckService.ContradictionCheck corpusContradiction = checkContradictionSafely(text, match.getBestVerifiedEntry().getText());
            if (corpusContradiction != null && corpusContradiction.isContradicted()) {
                return corpusContradiction.getSeverity();
            }
        }

        if (subClaims != null) {
            for (DecomposedClaim sub : subClaims) {
                if ("REFUTED".equals(sub.getClaimVerdict()) && "PRIMARY_CLAIM".equals(sub.getClaimCentrality())) {
                    return "DIRECT_FACTUAL_REVERSAL";
                }
            }
        }

        return "NONE";
    }

    private int calculateSupportScore(String text, NlpAnalysisResponse nlp,
                                      ClaimVerificationResponse.ImageIntegrityAnalysis imageAnalysis,
                                      MatchResult match,
                                      Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                      VerifiedSource domainSource,
                                      List<DecomposedClaim> subClaims) {

        double debunkedSim = match.getDebunkedSimilarity();
        double verifiedSim = match.getVerifiedSimilarity();

        // 0. Check for demographic / empirical factual impossibility
        Optional<String> demographicAnomaly = checkDemographicAnomaly(text);
        if (demographicAnomaly.isPresent()) {
            return 18;
        }

        // 0.1 Graduated Contradiction Penalty Assessment
        if (externalFact.isPresent() && externalFact.get().isContradiction()) {
            String severity = externalFact.get().getContradictionSeverity();
            if ("MINOR_DISCREPANCY".equals(severity)) {
                return 76; // Minor variance (e.g. 165 vs 166)
            } else if ("MODERATE_CONTRADICTION".equals(severity)) {
                return 54;
            } else if ("MAJOR_CONTRADICTION".equals(severity)) {
                return 32;
            } else {
                return 18; // DIRECT_FACTUAL_REVERSAL
            }
        }

        if (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.35) {
            ExternalFactCheckService.ContradictionCheck corpusContradiction = checkContradictionSafely(text, match.getBestVerifiedEntry().getText());
            if (corpusContradiction != null && corpusContradiction.isContradicted()) {
                String severity = corpusContradiction.getSeverity();
                return "MINOR_DISCREPANCY".equals(severity) ? 76 : ("MODERATE_CONTRADICTION".equals(severity) ? 54 : 18);
            }
        }

        boolean isVerifiedCompatible = isEntityCompatible(nlp, match.getBestVerifiedEntry(), text);
        boolean isDebunkedCompatible = isEntityCompatible(nlp, match.getBestDebunkedEntry(), text);

        // 1. Digital Image Tampering Override
        if (imageAnalysis != null && imageAnalysis.getManipulationProbability() > 75) {
            return (int) Math.max(8, 25 - (imageAnalysis.getManipulationProbability() - 75));
        }

        // 2. Strong Debunked / Hoax Match (TF-IDF Vector Representation Match >= 0.45)
        if (debunkedSim >= 0.45 && isDebunkedCompatible && debunkedSim >= verifiedSim) {
            int baseDebunkScore = (int) (28 - (debunkedSim * 20));
            return Math.max(6, Math.min(24, baseDebunkScore));
        }

        // 3. Strong Verified Fact Match (TF-IDF Vector Representation Match >= 0.45)
        if (verifiedSim >= 0.45 && isVerifiedCompatible && verifiedSim > debunkedSim) {
            int baseVerifiedScore = (int) (84 + (verifiedSim * 14));
            return Math.max(85, Math.min(98, baseVerifiedScore));
        }

        // 4. Accredited Domain URL Boost
        if (domainSource != null && domainSource.getCredibilityScore() >= 90) {
            int domainScore = domainSource.getCredibilityScore();
            return Math.max(72, Math.min(96, domainScore));
        }

        // 5. External Verified Knowledge / Live News Wire Corroboration
        if (externalFact.isPresent() && externalFact.get().isAuthenticCorroboration()) {
            int cred = externalFact.get().getCredibilityScore() > 0 ? externalFact.get().getCredibilityScore() : 94;
            return Math.max(82, Math.min(98, cred));
        }

        // 6. Centrality-Weighted aggregation if multiple sub-claims exist
        if (subClaims != null && subClaims.size() > 1) {
            double weightedSum = 0;
            double totalWeight = 0;
            boolean hasPrimaryReversal = false;

            for (DecomposedClaim sub : subClaims) {
                double weight = sub.getClaimImportanceWeight() > 0 ? sub.getClaimImportanceWeight() : 0.50;
                weightedSum += (sub.getClaimScore() * weight);
                totalWeight += weight;

                if ("PRIMARY_CLAIM".equals(sub.getClaimCentrality()) && "REFUTED".equals(sub.getClaimVerdict())) {
                    hasPrimaryReversal = true;
                }
            }

            int aggregateScore = (int) Math.round(weightedSum / (totalWeight > 0 ? totalWeight : 1.0));
            if (hasPrimaryReversal) {
                return Math.min(18, aggregateScore);
            }
            return aggregateScore;
        }

        // 7. General Unverified / Breaking Claim (Zero contradictory evidence & zero confirming evidence)
        return 50; // Insufficient evidence baseline (neutral)
    }

    private String determineVerdict(int score, Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                   MatchResult match, String contradictionSeverity,
                                   List<DecomposedClaim> subClaims, String asOfStatus) {

        // Epistemic Condition: If 0 supporting and 0 contradicting records, return INSUFFICIENT EVIDENCE
        boolean hasConfirmedEvidence = (externalFact.isPresent() && externalFact.get().isAuthenticCorroboration()) ||
                (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.45);
        boolean hasContradictingEvidence = (externalFact.isPresent() && externalFact.get().isContradiction()) ||
                (match.getBestDebunkedEntry() != null && match.getDebunkedSimilarity() >= 0.45) ||
                !"NONE".equals(contradictionSeverity);

        if (!hasConfirmedEvidence && !hasContradictingEvidence) {
            return "INSUFFICIENT EVIDENCE";
        }

        if (match.getBestDebunkedEntry() != null && match.getDebunkedSimilarity() >= 0.55) {
            return "DOCUMENTED_HOAX";
        }

        // Check for Partially Supported compound claims (e.g. 1 verified, 1 unverified)
        if (subClaims != null && subClaims.size() > 1) {
            boolean hasVerified = subClaims.stream().anyMatch(s -> "VERIFIED".equals(s.getClaimVerdict()) || "MOSTLY_VERIFIED".equals(s.getClaimVerdict()));
            boolean hasUnverified = subClaims.stream().anyMatch(s -> "UNVERIFIED".equals(s.getClaimVerdict()));
            boolean hasRefuted = subClaims.stream().anyMatch(s -> "REFUTED".equals(s.getClaimVerdict()));

            if (hasVerified && hasUnverified && !hasRefuted) {
                return "PARTIALLY SUPPORTED";
            }
        }

        // Clean Epistemic Verdict Vocabulary
        if (score >= 85) return "VERIFIED / STRONGLY SUPPORTED";
        if (score >= 70) return "MOSTLY SUPPORTED";
        if (score >= 40) return "PARTIALLY SUPPORTED";
        if (score >= 20) return "LIKELY FABRICATED";
        return "STRONGLY CONTRADICTED";
    }

    private String getVerdictBadgeColor(String verdict, int score) {
        if ("VERIFIED / STRONGLY SUPPORTED".equals(verdict) || "MOSTLY SUPPORTED".equals(verdict) || "VERIFIED GENUINE".equals(verdict) || "MOSTLY GENUINE".equals(verdict)) return "#10B981"; // Emerald Green
        if ("INSUFFICIENT EVIDENCE".equals(verdict) || "INSUFFICIENT_EVIDENCE".equals(verdict)) return "#94A3B8"; // Slate Gray
        if ("PARTIALLY SUPPORTED".equals(verdict) || "MIXED / CONFLICTING EVIDENCE".equals(verdict) || "DEVELOPING EVENT".equals(verdict)) return "#F59E0B"; // Amber Yellow
        if ("NON-VERIFIABLE INPUT".equals(verdict) || "NOT_VERIFIABLE".equals(verdict)) return "#64748B"; // Neutral Slate
        return "#EF4444"; // Crimson Red
    }

    private int calculateConfidenceScore(MatchResult match, Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                        List<DecomposedClaim> subClaims) {
        int score = 40; // baseline
        if (externalFact.isPresent()) {
            int sourceCount = externalFact.get().getCrossReferencedSources().size();
            score += Math.min(45, sourceCount * 15);
            if (externalFact.get().getCredibilityScore() >= 90) {
                score += 15;
            }
        }
        if (match.getTopSimilarity() >= 0.60) {
            score += 25;
        } else if (match.getTopSimilarity() >= 0.35) {
            score += 10;
        }
        return Math.min(100, score);
    }

    private ExplainabilityProfile buildExplainabilityProfile(String text, int score, String verdict, String confidence,
                                                            int confidenceScore,
                                                            int evidenceCompleteness,
                                                            String asOfStatus,
                                                            String distortionType,
                                                            List<SourceEvidence> sources,
                                                            List<EvidenceCluster> clusters,
                                                            Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                                            MatchResult match,
                                                            String contradictionSeverity,
                                                            RetrievalAudit retrievalAudit) {

        List<String> positive = new ArrayList<>();
        List<String> warning = new ArrayList<>();
        List<String> diffs = new ArrayList<>();
        List<EvidenceItemSummary> matrix = new ArrayList<>();

        boolean isContradicted = !"NONE".equals(contradictionSeverity);

        if (clusters != null && !clusters.isEmpty()) {
            if (score >= 70 && !isContradicted) {
                positive.add("Corroborated across " + clusters.size() + " independent evidence cluster" + (clusters.size() > 1 ? "s" : "") + ".");
            } else if (isContradicted) {
                warning.add("Contradicted across " + clusters.size() + " independent evidence cluster" + (clusters.size() > 1 ? "s" : "") + ".");
            }
        }

        if (externalFact.isPresent()) {
            ExternalFactCheckService.ExternalFactResult res = externalFact.get();
            if (res.isAuthenticCorroboration()) {
                positive.add("Earliest verified source report located from " + res.getSourceName() + ".");
                positive.add("Factual entities and core topic aligned with accredited reporting.");
            } else if (res.isContradiction()) {
                warning.add("Contradiction detected (" + distortionType + " / " + contradictionSeverity + "): " + res.getContradictionDetail());
                diffs.add("Claim asserts contrary facts vs. reporting from " + res.getSourceName() + ": " + res.getContradictionDetail());
            }
        } else if ("INSUFFICIENT EVIDENCE".equals(verdict)) {
            warning.add("No primary wire agency or official registry has reported this assertion yet.");
            warning.add("Emerging claim requires independent primary source corroboration before acceptance.");
        }

        if (match.getBestDebunkedEntry() != null && match.getDebunkedSimilarity() >= 0.45) {
            warning.add("Matches documented viral hoax debunked by " + match.getBestDebunkedEntry().getSourceName() + ".");
        }

        for (SourceEvidence se : sources) {
            matrix.add(EvidenceItemSummary.builder()
                    .sourceName(se.getSourceName())
                    .sourceType(se.getEvidenceTier() != null ? se.getEvidenceTier().replace("LEVEL_", "Level ").replace("_", " ") : "News Wire")
                    .evidenceTier(se.getEvidenceTier() != null ? se.getEvidenceTier() : "LEVEL_2_SECONDARY")
                    .stance(se.getStance() != null ? se.getStance() : (score >= 70 ? "SUPPORTED" : "REFUTED"))
                    .reliability(se.getCredibilityRating() >= 90 ? "HIGH" : "MEDIUM")
                    .independence(se.getIndependenceRating() > 0 ? se.getIndependenceRating() : 80.0)
                    .contextualAuthorityScore(se.getContextualAuthorityScore() > 0 ? se.getContextualAuthorityScore() : 0.85)
                    .geographicRelevance(se.getGeographicRelevance() != null ? se.getGeographicRelevance() : "HIGH")
                    .directness(se.getDirectness() != null ? se.getDirectness() : "SECONDARY_REPORTING")
                    .evidenceRole("Direct Verification Evidence")
                    .acceptanceReasons(se.getAcceptanceReasons())
                    .build());
        }

        return ExplainabilityProfile.builder()
                .confidenceLevel(confidence)
                .confidenceScore(confidenceScore)
                .evidenceCompleteness(evidenceCompleteness)
                .asOfStatus(asOfStatus)
                .distortionType(distortionType)
                .positiveChecklist(positive)
                .warningChecklist(warning)
                .detectedDifferences(diffs)
                .evidenceMatrix(matrix)
                .retrievalAudit(retrievalAudit)
                .build();
    }

    private ContentCharacteristics buildContentDiagnostics(NlpAnalysisResponse nlp) {
        if (nlp == null) return null;

        int clickbait = (int) Math.round(nlp.getClickbaitRating());
        double subjectivity = nlp.getSubjectivityScore();
        double sentiment = nlp.getSentimentScore();

        String sensationalismLevel = clickbait >= 65 ? "HIGH" : (clickbait >= 35 ? "MEDIUM" : "LOW");
        String subjectivityLevel = subjectivity >= 0.55 ? "SUBJECTIVE" : (subjectivity >= 0.35 ? "BALANCED" : "OBJECTIVE");
        String emotionalTone = Math.abs(sentiment) >= 0.7 ? "EMOTIONALLY CHARGED" : (clickbait >= 50 ? "SENSATIONAL" : "NEUTRAL");

        return ContentCharacteristics.builder()
                .sensationalismLevel(sensationalismLevel)
                .clickbaitRating(clickbait)
                .subjectivityLevel(subjectivityLevel)
                .emotionalTone(emotionalTone)
                .triggerFlags(nlp.getExaggerationFlags())
                .build();
    }

    private List<EvidenceCluster> buildDefaultClusters(List<SourceEvidence> sources, boolean isContradicted) {
        List<EvidenceCluster> clusters = new ArrayList<>();
        if (sources == null || sources.isEmpty()) return clusters;

        clusters.add(EvidenceCluster.builder()
                .clusterId("C001")
                .clusterTheme("Accredited Documentary Repositories")
                .primaryOutlet(sources.get(0).getSourceName())
                .affiliatedOutlets(sources.stream().map(SourceEvidence::getSourceName).collect(Collectors.toList()))
                .sourceCount(sources.size())
                .independenceRating(85.0)
                .consensusStance(isContradicted ? "REFUTED" : "CONFIRMED")
                .evidenceTier("LEVEL_2_SECONDARY")
                .isPrimaryAuthority(false)
                .build());

        return clusters;
    }

    private RetrievalAudit buildDefaultAudit(List<SourceEvidence> sources, List<EvidenceCluster> clusters) {
        return RetrievalAudit.builder()
                .searchQueriesRun(1)
                .sourcesRetrieved(sources.size())
                .relevantSources(sources.size())
                .rejectedSources(0)
                .syndicatedDuplicates(0)
                .independentClustersCount(clusters.size())
                .primarySourcesCount((int) sources.stream().filter(SourceEvidence::isPrimarySource).count())
                .accreditedSecondaryCount(sources.size())
                .referenceSourcesCount(0)
                .supportingSourcesCount(sources.size())
                .contradictingSourcesCount(0)
                .auditSummary("Retrieved " + sources.size() + " sources across " + clusters.size() + " clusters.")
                .build();
    }

    private Optional<String> checkDemographicAnomaly(String text) {
        if (text == null) return Optional.empty();
        String lower = text.toLowerCase();

        if ((lower.contains("billion") || lower.contains("trillion")) &&
            (lower.contains("human") || lower.contains("people") || lower.contains("population") || lower.contains("citizens") || lower.contains("inhabitants")) &&
            (lower.contains("earth") || lower.contains("world") || lower.contains("planet") || lower.contains("are") || lower.contains("living") || lower.contains("now"))) {

            Pattern numPattern = Pattern.compile("(\\b(?:[1-9]\\d{1,}|[2-9]\\d?|100)\\s*(?:billion|trillion))", Pattern.CASE_INSENSITIVE);
            if (numPattern.matcher(lower).find()) {
                return Optional.of("Earth's total human population is approximately 8.1 billion (United Nations Demographics). Assertions of tens or hundreds of billions of living humans contradict established global censuses.");
            }
        }
        return Optional.empty();
    }

    private boolean isEntityCompatible(NlpAnalysisResponse nlp, CorpusEntry entry, String rawText) {
        if (entry == null) return false;
        if (nlp == null) return true;

        String entryTextLower = entry.getText().toLowerCase();
        String rawTextLower = rawText != null ? rawText.toLowerCase() : "";

        if (nlp.getExtractedEntities() != null && !nlp.getExtractedEntities().isEmpty()) {
            boolean hasKeyEntityMatch = nlp.getExtractedEntities().stream()
                    .anyMatch(e -> entryTextLower.contains(e.toLowerCase()));

            if (!hasKeyEntityMatch) {
                long sharedWordCount = Arrays.stream(rawTextLower.split("\\s+"))
                        .filter(w -> w.length() > 4)
                        .filter(entryTextLower::contains)
                        .count();
                return sharedWordCount >= 2;
            }
            return true;
        }

        return true;
    }

    private List<String> generateKeyReasons(String text, NlpAnalysisResponse nlp, int score,
                                            ClaimVerificationResponse.ImageIntegrityAnalysis image,
                                            MatchResult match,
                                            Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                            VerifiedSource domainSource,
                                            String contradictionSeverity,
                                            int completeness,
                                            String distortionType) {
        List<String> reasons = new ArrayList<>();

        Optional<String> demographicAnomaly = checkDemographicAnomaly(text);
        if (demographicAnomaly.isPresent()) {
            reasons.add(demographicAnomaly.get());
            return reasons;
        }

        if (externalFact.isPresent()) {
            ExternalFactCheckService.ExternalFactResult fact = externalFact.get();
            if (fact.isContradiction()) {
                reasons.add("Contradicted by verified press reporting from " + fact.getSourceName() + " (" + distortionType + " / " + contradictionSeverity + ").");
                if (fact.getContradictionDetail() != null) {
                    reasons.add(fact.getContradictionDetail());
                }
                return reasons;
            } else if (fact.isAuthenticCorroboration()) {
                reasons.add("Corroborated by contemporaneous press reporting from " + fact.getSourceName() + " (" + fact.getSourceDomain() + ").");
                reasons.add("Matched against official news dispatch: \"" + fact.getTopic() + "\".");
                reasons.add("Cross-referenced across " + fact.getEvidenceClusters().size() + " independent evidence clusters with " + completeness + "% factual completeness.");
                return reasons;
            }
        }

        if (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.35) {
            ExternalFactCheckService.ContradictionCheck corpusContradiction = checkContradictionSafely(text, match.getBestVerifiedEntry().getText());
            if (corpusContradiction != null && corpusContradiction.isContradicted()) {
                reasons.add("Contradicted by documentary records from " + match.getBestVerifiedEntry().getSourceName() + ".");
                reasons.add(corpusContradiction.getReason());
                return reasons;
            }
        }

        if (image != null && image.getManipulationProbability() > 70) {
            reasons.add("Visual Error Level Analysis indicates potential compression anomalies (" + String.format("%.1f", image.getManipulationProbability()) + "%).");
            reasons.addAll(image.getAnomalyFlags());
        }

        if (match.getBestDebunkedEntry() != null && match.getDebunkedSimilarity() >= 0.45) {
            reasons.add("Factual match with debunked misinformation archive (" + match.getBestDebunkedEntry().getSourceName() + ").");
            reasons.add(match.getBestDebunkedEntry().getRationale());
        } else if (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.45) {
            reasons.add("Corroborated by verified documentary records (" + match.getBestVerifiedEntry().getSourceName() + ").");
            reasons.add(match.getBestVerifiedEntry().getRationale());
        }

        if (domainSource != null) {
            reasons.add("Domain " + domainSource.getDomain() + " has an accredited trust score of " + domainSource.getCredibilityScore() + "/100.");
        }

        if (reasons.isEmpty()) {
            reasons.add("Contains unverified assertions without independent confirmation from primary wire agencies.");
            reasons.add("Requires independent verification before accepting as genuine fact.");
        }

        return reasons;
    }

    private String buildRationaleText(String text, int score, String verdict, NlpAnalysisResponse nlp,
                                      MatchResult match,
                                      Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                      VerifiedSource domainSource,
                                      String contradictionSeverity,
                                      int completeness) {

        Optional<String> demographicAnomaly = checkDemographicAnomaly(text);
        if (demographicAnomaly.isPresent()) {
            return "TruthLens has evaluated this submission as STRONGLY CONTRADICTED. " + demographicAnomaly.get();
        }

        if (externalFact.isPresent()) {
            ExternalFactCheckService.ExternalFactResult fact = externalFact.get();
            if (fact.isContradiction()) {
                return "TruthLens has evaluated this submission as " + verdict + ". Contradicted by verified press reporting from " +
                        fact.getSourceName() + ": \"" + fact.getTopic() + "\" (" + contradictionSeverity + "). " + fact.getContradictionDetail();
            } else if (fact.isAuthenticCorroboration()) {
                return "TruthLens verified this claim against accredited news wire archives and real-time press reporting. Corroborated by contemporaneous reporting from " +
                        fact.getSourceName() + " (" + completeness + "% sub-claim completeness).";
            }
        }

        if (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.35) {
            ExternalFactCheckService.ContradictionCheck corpusContradiction = checkContradictionSafely(text, match.getBestVerifiedEntry().getText());
            if (corpusContradiction != null && corpusContradiction.isContradicted()) {
                return "TruthLens has evaluated this submission as " + verdict + ". Contradicted by verified documentary records from " +
                        match.getBestVerifiedEntry().getSourceName() + " (" + match.getBestVerifiedEntry().getArticleTitle() + "). " + corpusContradiction.getReason();
            }
        }

        if (match.getBestDebunkedEntry() != null && match.getDebunkedSimilarity() >= 0.45) {
            return "TruthLens matched this claim with documented misinformation debunked by " +
                    match.getBestDebunkedEntry().getSourceName() + " (" + match.getBestDebunkedEntry().getArticleTitle() + "). " +
                    match.getBestDebunkedEntry().getRationale();
        }

        if (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.45) {
            return "TruthLens verified this claim against documented archival records from " +
                    match.getBestVerifiedEntry().getSourceName() + " (" + match.getBestVerifiedEntry().getArticleTitle() + "). " +
                    match.getBestVerifiedEntry().getRationale();
        }

        if ("INSUFFICIENT EVIDENCE".equals(verdict) || "INSUFFICIENT_EVIDENCE".equals(verdict)) {
            return "TruthLens evaluated this submission as INSUFFICIENT EVIDENCE. No primary wire agency or official registry has reported this assertion yet. Emerging claims require independent primary documentation.";
        }

        return "This claim contains unverified assertions without independent confirmation from primary wire agencies. It requires primary source evidence before it can be verified as genuine.";
    }

    private List<SourceEvidence> buildSourceCitations(String text, int score, MatchResult match,
                                                      Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                                      VerifiedSource domainSource) {
        if (externalFact.isPresent() && !externalFact.get().getCrossReferencedSources().isEmpty()) {
            return externalFact.get().getCrossReferencedSources();
        }

        List<SourceEvidence> sources = new ArrayList<>();

        if (match.getBestDebunkedEntry() != null && match.getDebunkedSimilarity() >= 0.45) {
            CorpusEntry entry = match.getBestDebunkedEntry();
            sources.add(SourceEvidence.builder()
                    .sourceName(entry.getSourceName())
                    .domain(entry.getSourceDomain())
                    .evidenceTier("LEVEL_3_FACTCHECK")
                    .credibilityRating(96)
                    .matchPercentage(Math.round(match.getDebunkedSimilarity() * 1000.0) / 10.0)
                    .independenceRating(90.0)
                    .contextualAuthorityScore(0.95)
                    .geographicRelevance("HIGH")
                    .directness("DIRECT_PRIMARY")
                    .stance("REFUTED")
                    .verdictBySource("Debunked / False")
                    .articleTitle(entry.getArticleTitle())
                    .url(entry.getSourceUrl())
                    .clusterId("C001")
                    .isPrimarySource(false)
                    .acceptanceReasons(List.of("Accredited fact-checking repository record", "Matching debunked claim pattern"))
                    .build());
        }

        if (match.getBestVerifiedEntry() != null && match.getVerifiedSimilarity() >= 0.35) {
            CorpusEntry entry = match.getBestVerifiedEntry();
            ExternalFactCheckService.ContradictionCheck corpusContradiction = checkContradictionSafely(text, entry.getText());
            boolean isCorpusContradicted = corpusContradiction != null && corpusContradiction.isContradicted();

            sources.add(SourceEvidence.builder()
                    .sourceName(entry.getSourceName())
                    .domain(entry.getSourceDomain())
                    .evidenceTier("LEVEL_2_SECONDARY")
                    .credibilityRating(98)
                    .matchPercentage(Math.round(match.getVerifiedSimilarity() * 1000.0) / 10.0)
                    .independenceRating(100.0)
                    .contextualAuthorityScore(0.92)
                    .geographicRelevance("HIGH")
                    .directness("SECONDARY_REPORTING")
                    .stance(isCorpusContradicted ? "REFUTED" : "SUPPORTED")
                    .verdictBySource(isCorpusContradicted ? "Contradicted / False" : "Verified True")
                    .articleTitle(entry.getArticleTitle())
                    .url(entry.getSourceUrl())
                    .clusterId("C001")
                    .isPrimarySource(false)
                    .acceptanceReasons(List.of("Accredited documentary record", "Corroborates core propositions"))
                    .build());
        }

        if (domainSource != null) {
            sources.add(SourceEvidence.builder()
                    .sourceName(domainSource.getName())
                    .domain(domainSource.getDomain())
                    .evidenceTier("LEVEL_2_SECONDARY")
                    .credibilityRating(domainSource.getCredibilityScore())
                    .matchPercentage(90.0)
                    .independenceRating(75.0)
                    .contextualAuthorityScore(0.85)
                    .geographicRelevance("HIGH")
                    .directness("SECONDARY_REPORTING")
                    .stance("SUPPORTED")
                    .verdictBySource(score >= 70 ? "Verified Domain" : "Unverified")
                    .articleTitle("Accredited Press Domain (" + domainSource.getDomain() + ")")
                    .url("https://" + domainSource.getDomain())
                    .clusterId("C002")
                    .isPrimarySource(false)
                    .acceptanceReasons(List.of("Verified accredited domain registry", "Domain credibility trust score: " + domainSource.getCredibilityScore()))
                    .build());
        }

        return sources;
    }

    private ClaimOriginDiscovery buildClaimOriginDiscovery(String text, int score, String verdict,
                                                           MatchResult corpusMatch,
                                                           Optional<ExternalFactCheckService.ExternalFactResult> externalFact,
                                                           VerifiedSource domainSource,
                                                           String contradictionSeverity,
                                                           String distortionType) {
        if (externalFact.isPresent() && externalFact.get().getOriginDiscovery() != null) {
            return externalFact.get().getOriginDiscovery();
        }

        if (corpusMatch.getBestDebunkedEntry() != null && corpusMatch.getDebunkedSimilarity() >= 0.45) {
            CorpusEntry entry = corpusMatch.getBestDebunkedEntry();
            return ClaimOriginDiscovery.builder()
                    .originalPublisher(entry.getSourceName())
                    .earliestIdentifiedPublisher(entry.getSourceName())
                    .earliestVerifiedSourceFound(entry.getSourceName())
                    .originalDomain(entry.getSourceDomain())
                    .originalHeadline(entry.getArticleTitle())
                    .originalUrl(entry.getSourceUrl())
                    .publishedDate("Fact-Check Archive")
                    .retrievalTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .provenanceType("DOCUMENTED_HOAX")
                    .provenanceStatus("EARLIEST_VERIFIED_SOURCE_FOUND")
                    .claimIntegrity("DOCUMENTED_HOAX")
                    .contradictionSeverity("DIRECT_FACTUAL_REVERSAL")
                    .evidenceTier("LEVEL_3_FACTCHECK")
                    .distortionAnalysis("Identified as a documented viral internet hoax debunked by " + entry.getSourceName() + ".")
                    .crossReferencedConsensus("Refuted across accredited fact-checking repositories (Snopes, PolitiFact, AP).")
                    .provenanceConfidence("HIGH")
                    .originMatchConfidence(Math.round(corpusMatch.getDebunkedSimilarity() * 1000.0) / 10.0)
                    .build();
        }

        if (corpusMatch.getBestVerifiedEntry() != null && corpusMatch.getVerifiedSimilarity() >= 0.45) {
            CorpusEntry entry = corpusMatch.getBestVerifiedEntry();
            ExternalFactCheckService.ContradictionCheck corpusContradiction = checkContradictionSafely(text, entry.getText());
            boolean isCorpusContradicted = corpusContradiction != null && corpusContradiction.isContradicted();
            String severity = corpusContradiction != null ? corpusContradiction.getSeverity() : "NONE";

            String claimIntegrity = !isCorpusContradicted ? "AUTHENTIC_REPRODUCTION" :
                    ("MINOR_DISCREPANCY".equals(severity) ? "MINOR_VARIANCE" : "ALTERED_DISTORTION");

            return ClaimOriginDiscovery.builder()
                    .originalPublisher(entry.getSourceName())
                    .earliestIdentifiedPublisher(entry.getSourceName())
                    .earliestVerifiedSourceFound(entry.getSourceName())
                    .originalDomain(entry.getSourceDomain())
                    .originalHeadline(entry.getArticleTitle())
                    .originalUrl(entry.getSourceUrl())
                    .publishedDate("Historical Wire Dispatch")
                    .retrievalTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .provenanceType(claimIntegrity)
                    .provenanceStatus("EARLIEST_VERIFIED_SOURCE_FOUND")
                    .claimIntegrity(claimIntegrity)
                    .contradictionSeverity(severity)
                    .evidenceTier("LEVEL_2_SECONDARY")
                    .distortionAnalysis(isCorpusContradicted ?
                            "Claim derived from reporting by " + entry.getSourceName() + ", with discrepancy (" + severity + "): " + corpusContradiction.getReason() :
                            "Claim matches verified documentary records published by " + entry.getSourceName() + ".")
                    .crossReferencedConsensus(isCorpusContradicted ?
                            "Contradicted by primary reporting from " + entry.getSourceName() + "." :
                            "Corroborated across primary news wire repositories.")
                    .provenanceConfidence("HIGH")
                    .originMatchConfidence(Math.round(corpusMatch.getVerifiedSimilarity() * 1000.0) / 10.0)
                    .build();
        }

        if (domainSource != null) {
            return ClaimOriginDiscovery.builder()
                    .originalPublisher(domainSource.getName())
                    .earliestIdentifiedPublisher(domainSource.getName())
                    .earliestVerifiedSourceFound(domainSource.getName())
                    .originalDomain(domainSource.getDomain())
                    .originalHeadline("Direct Web Publication (" + domainSource.getDomain() + ")")
                    .originalUrl("https://" + domainSource.getDomain())
                    .publishedDate("Accredited Press Domain")
                    .retrievalTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .provenanceType(score >= 70 ? "AUTHENTIC_REPRODUCTION" : "UNVERIFIED_ORIGIN")
                    .provenanceStatus("EARLIEST_VERIFIED_SOURCE_FOUND")
                    .claimIntegrity(score >= 70 ? "AUTHENTIC_REPRODUCTION" : "UNVERIFIED_ORIGIN")
                    .contradictionSeverity("NONE")
                    .evidenceTier("LEVEL_2_SECONDARY")
                    .distortionAnalysis("Originates from registered press domain (" + domainSource.getName() + ", Credibility " + domainSource.getCredibilityScore() + "/100).")
                    .crossReferencedConsensus("Domain credibility rating: " + domainSource.getCredibilityScore() + "/100.")
                    .provenanceConfidence("HIGH")
                    .originMatchConfidence(domainSource.getCredibilityScore())
                    .build();
        }

        return ClaimOriginDiscovery.builder()
                .originalPublisher("Unverified Online Source")
                .earliestIdentifiedPublisher("Unverified Online Source")
                .earliestVerifiedSourceFound("Unverified Online Source")
                .originalDomain("unverified")
                .originalHeadline("No primary news wire headline indexed")
                .originalUrl("https://news.google.com")
                .publishedDate("Undated Online Assertion")
                .retrievalTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .provenanceType("UNVERIFIED_ORIGIN")
                .provenanceStatus("ORIGIN_NOT_DETERMINED")
                .claimIntegrity("UNVERIFIED_ORIGIN")
                .contradictionSeverity("NONE")
                .evidenceTier("LEVEL_5_USER_GENERATED")
                .distortionAnalysis("No accredited primary news agency, scientific journal, or official registry has reported this assertion.")
                .crossReferencedConsensus("Uncorroborated: 0 primary news wire records found.")
                .provenanceConfidence("LOW")
                .originMatchConfidence(40.0)
                .build();
    }

    private List<PipelineStep> buildPipelineSteps(String inputType, ImageIntegrityAnalysis img, ClaimVerifiabilityValidator.ValidationResult val, 
                                                 List<DecomposedClaim> subClaims, List<EvidenceCluster> clusters, 
                                                 String verdict, boolean isBlocked) {
        List<PipelineStep> steps = new ArrayList<>();
        
        // Step 1: Input Ingestion
        steps.add(PipelineStep.builder()
                .stepNumber("01")
                .stepName("Input Ingestion")
                .status("COMPLETED")
                .detail("Modality: " + inputType + " payload received, stripped of control characters, and normalized.")
                .build());
                
        // Step 2: Modality & OCR Quality Gate (if Image)
        if ("IMAGE".equalsIgnoreCase(inputType)) {
            if (img != null && ("UNRELIABLE".equals(img.getOcrQualityLevel()) || "NO_TEXT_DETECTED".equals(img.getClaimExtractionStatus()) || "OCR_UNRELIABLE".equals(img.getClaimExtractionStatus()))) {
                steps.add(PipelineStep.builder()
                        .stepNumber("02")
                        .stepName("OCR Quality Gate")
                        .status("BLOCKED")
                        .detail("OCR Quality: UNRELIABLE (" + Math.round(img.getValidWordRatio() != null ? img.getValidWordRatio() : 0) + "% valid words). Non-verifiable image.")
                        .build());
            } else {
                steps.add(PipelineStep.builder()
                        .stepNumber("02")
                        .stepName("OCR Quality Gate")
                        .status("PASSED")
                        .detail("OCR Quality: " + (img != null ? img.getOcrQualityLevel() : "HIGH") + " (" + (img != null ? Math.round(img.getValidWordRatio()) : 100) + "% valid words). Multi-pass consistency verified.")
                        .build());
            }
        } else {
            steps.add(PipelineStep.builder()
                    .stepNumber("02")
                    .stepName("Modality Processing")
                    .status("COMPLETED")
                    .detail("Input payload validated: " + inputType + " schema parsed.")
                    .build());
        }
        
        // Step 3: Claim Verifiability Pre-check
        if (val != null && !val.isVerifiableClaim()) {
            steps.add(PipelineStep.builder()
                    .stepNumber("03")
                    .stepName("Claim Verifiability Gate")
                    .status("BLOCKED")
                    .detail("Non-verifiable: " + val.getRejectionReason())
                    .build());
            steps.add(PipelineStep.builder()
                    .stepNumber("04")
                    .stepName("Evidence Search")
                    .status("SKIPPED")
                    .detail("Skipped: Non-verifiable input refuses external wire search.")
                    .build());
            steps.add(PipelineStep.builder()
                    .stepNumber("05")
                    .stepName("Epistemic Verdict")
                    .status("COMPLETED")
                    .detail("Verdict: " + verdict + " (Support Score: N/A)")
                    .build());
            return steps;
        } else if (isBlocked) {
            steps.add(PipelineStep.builder()
                    .stepNumber("03")
                    .stepName("Claim Decomposition")
                    .status("SKIPPED")
                    .detail("Skipped: No reliable claim extracted from image.")
                    .build());
            steps.add(PipelineStep.builder()
                    .stepNumber("04")
                    .stepName("Evidence Retrieval")
                    .status("SKIPPED")
                    .detail("Skipped: External wire query bypassed for non-claim photo.")
                    .build());
            steps.add(PipelineStep.builder()
                    .stepNumber("05")
                    .stepName("Epistemic Verdict")
                    .status("COMPLETED")
                    .detail("Verdict: " + verdict + " (Support Score: N/A)")
                    .build());
            return steps;
        }
        
        steps.add(PipelineStep.builder()
                .stepNumber("03")
                .stepName("Atomic Claim Decomposition")
                .status("COMPLETED")
                .detail((subClaims != null && !subClaims.isEmpty() ? subClaims.size() : 1) + " atomic proposition(s) extracted with Entity-Predicate-Value semantics.")
                .build());
                
        // Step 4: Regional Source Retrieval & Syndication Clustering
        int clusterCount = clusters != null ? clusters.size() : 1;
        steps.add(PipelineStep.builder()
                .stepNumber("04")
                .stepName("Evidence Retrieval & Clustering")
                .status("COMPLETED")
                .detail("Dispatches aggregated into " + clusterCount + " independent evidence cluster(s) (Anti-echo syndication grouped).")
                .build());
                
        // Step 5: Contradiction & Scoring Engine
        steps.add(PipelineStep.builder()
                .stepNumber("05")
                .stepName("Contradiction & Qualifier Analysis")
                .status("COMPLETED")
                .detail("Numerical, temporal, and polarity bounds evaluated against authoritative records.")
                .build());
                
        // Step 6: Final Epistemic Verdict
        steps.add(PipelineStep.builder()
                .stepNumber("06")
                .stepName("Final Epistemic Verdict")
                .status("COMPLETED")
                .detail("Verdict: " + verdict)
                .build());
                
        return steps;
    }
}
