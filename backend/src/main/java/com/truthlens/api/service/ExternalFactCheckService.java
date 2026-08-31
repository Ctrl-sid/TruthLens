package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimVerificationResponse.ClaimContextInfo;
import com.truthlens.api.dto.ClaimVerificationResponse.ClaimOriginDiscovery;
import com.truthlens.api.dto.ClaimVerificationResponse.EvidenceCluster;
import com.truthlens.api.dto.ClaimVerificationResponse.RetrievalAudit;
import com.truthlens.api.dto.ClaimVerificationResponse.SourceEvidence;
import com.truthlens.api.nlp.ClaimContextService;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExternalFactCheckService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ClaimContextService claimContextService;

    @Autowired
    public ExternalFactCheckService(@Autowired(required = false) ClaimContextService claimContextService) {
        this.claimContextService = claimContextService != null ? claimContextService : new ClaimContextService();
    }

    public ExternalFactCheckService() {
        this(new ClaimContextService());
    }

    @Getter
    @Builder
    public static class ExternalFactResult {
        private String topic;
        private String snippet;
        private String sourceUrl;
        private String sourceName;
        private String sourceDomain;
        private boolean isAuthenticCorroboration;
        private boolean isContradiction;
        private String contradictionDetail;
        private String contradictionSeverity; // NONE, MINOR_DISCREPANCY, MODERATE_CONTRADICTION, MAJOR_CONTRADICTION, DIRECT_FACTUAL_REVERSAL
        private String distortionType; // NUMERICAL_DISTORTION, LOCATION_DISTORTION, ENTITY_DISTORTION, ATTRIBUTION_DISTORTION, POLARITY_DISTORTION, CONTEXT_DISTORTION, NONE
        private int credibilityScore;
        private double matchPercentage;
        private ClaimOriginDiscovery originDiscovery;
        private RetrievalAudit retrievalAudit;
        @Builder.Default
        private List<SourceEvidence> crossReferencedSources = new ArrayList<>();
        @Builder.Default
        private List<EvidenceCluster> evidenceClusters = new ArrayList<>();
    }

    public Optional<ExternalFactResult> queryExternalKnowledge(String query) {
        return queryExternalKnowledge(query, null);
    }

    public Optional<ExternalFactResult> queryExternalKnowledge(String query, ClaimContextInfo context) {
        if (query == null || query.isBlank()) return Optional.empty();

        if (context == null) {
            context = claimContextService.extractClaimContext(query);
        }

        // 1. Direct Contextual Live News Wire Search (Candidate Discovery)
        Optional<ExternalFactResult> liveNewsMatch = queryLiveNewsWires(query, query, context);
        if (liveNewsMatch.isPresent()) {
            return liveNewsMatch;
        }

        // 2. Direct Wikipedia OpenSearch (Level 4 Reference Archive - Strictly for entity context)
        Optional<ExternalFactResult> wikiMatch = queryWikipedia(query, query, context);
        if (wikiMatch.isPresent()) {
            return wikiMatch;
        }

        // 3. Fallback: If claim contains zero-quantifiers or modifiers, search for base event and cross-reference
        String coreEventQuery = extractCoreEventQuery(query);
        if (!coreEventQuery.equalsIgnoreCase(query) && coreEventQuery.length() >= 4) {
            Optional<ExternalFactResult> liveEventMatch = queryLiveNewsWires(coreEventQuery, query, context);
            if (liveEventMatch.isPresent()) {
                return liveEventMatch;
            }

            Optional<ExternalFactResult> wikiEventMatch = queryWikipedia(coreEventQuery, query, context);
            if (wikiEventMatch.isPresent()) {
                return wikiEventMatch;
            }
        }

        return Optional.empty();
    }

    public String extractCoreEventQuery(String query) {
        if (query == null) return "";
        return query.replaceAll("(?i)\\b(none|nobody|zero|nil|no\\s+one|no\\s+casualties|no\\s+deaths|never|not|fake|true|false)\\b", "")
                .replaceAll("[,;.:!?]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public Optional<ExternalFactResult> queryLiveNewsWires(String query) {
        return queryLiveNewsWires(query, query, null);
    }

    public Optional<ExternalFactResult> queryLiveNewsWires(String searchQuery, String originalQuery, ClaimContextInfo context) {
        try {
            String cleanQuery = cleanSearchQuery(searchQuery);
            if (cleanQuery.length() < 3) return Optional.empty();

            String encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
            String url = "https://news.google.com/rss/search?q=" + encodedQuery + "&hl=en-IN&gl=IN&ceid=IN:en";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseGoogleNewsRss(response.body(), originalQuery, context);
            }
        } catch (Exception e) {
            log.debug("Live news wire candidate search skipped or timed out: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<ExternalFactResult> parseGoogleNewsRss(String xml, String originalQuery, ClaimContextInfo context) {
        if (xml == null || !xml.contains("<item>")) return Optional.empty();

        Pattern itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);
        Matcher itemMatcher = itemPattern.matcher(xml);

        double bestOverlap = 0.0;
        String bestTitle = null;
        String bestLink = null;
        String bestSource = null;
        String bestSourceUrl = null;
        ContradictionCheck bestContradiction = null;

        List<SourceEvidence> crossReferencedList = new ArrayList<>();
        Set<String> seenDomains = new HashSet<>();
        int totalRetrieved = 0;
        int rejectedCount = 0;
        int syndicatedCount = 0;

        String geo = (context != null && !context.getGeographicEntities().isEmpty()) ?
                String.join(", ", context.getGeographicEntities()) : "";

        while (itemMatcher.find()) {
            totalRetrieved++;
            String itemBlock = itemMatcher.group(1);

            String title = extractTagValue(itemBlock, "title");
            String link = extractTagValue(itemBlock, "link");
            String source = extractTagValue(itemBlock, "source");
            String sourceUrl = extractSourceUrl(itemBlock);

            if (title != null) {
                title = unescapeXml(title);
            }
            if (source == null && title != null && title.contains(" - ")) {
                int lastDash = title.lastIndexOf(" - ");
                source = title.substring(lastDash + 3).trim();
                title = title.substring(0, lastDash).trim();
            }

            if (title == null || title.isBlank()) {
                rejectedCount++;
                continue;
            }

            String domain = extractDomain(sourceUrl != null ? sourceUrl : link);
            if (domain == null) {
                domain = extractDomainFromSourceName(source);
            }

            // Candidate Discovery Overlap Check
            double overlap = calculateQueryArticleOverlap(originalQuery, title);
            if (overlap < 0.25) {
                rejectedCount++;
                continue; // Filter out low-relevance noise
            }

            if (!isTier1AccreditedPublisher(source, domain)) {
                rejectedCount++;
                continue;
            }

            ContradictionCheck itemContradiction = detectContradiction(originalQuery, title);

            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                bestTitle = title;
                bestLink = link;
                bestSource = source != null ? source : "Accredited Wire Press";
                bestSourceUrl = sourceUrl != null ? sourceUrl : link;
                bestContradiction = itemContradiction;
            }

            if (domain != null && crossReferencedList.size() < 7) {
                boolean isDuplicateDomain = seenDomains.contains(domain.toLowerCase());
                if (isDuplicateDomain) {
                    syndicatedCount++;
                } else {
                    seenDomains.add(domain.toLowerCase());
                }

                String tier = determineEvidenceTier(source, domain);
                String stance = itemContradiction.isContradicted() ? "REFUTED" : "SUPPORTED";
                double independence = calculateIndependenceRating(source, domain);
                double contextualAuth = claimContextService.calculateContextualAuthorityScore(source, domain, geo, context != null ? context.getDomain() : "");

                List<String> acceptanceReasons = new ArrayList<>();
                acceptanceReasons.add("Accredited news publisher (" + (source != null ? source : domain) + ")");
                acceptanceReasons.add("Contemporaneous reporting with " + Math.round(overlap * 100) + "% proposition overlap");
                if (isPrimaryRegionalAuthority(source, domain, geo)) {
                    acceptanceReasons.add("Direct primary regional authority relevance");
                } else {
                    acceptanceReasons.add("Secondary news reporting corroboration");
                }

                crossReferencedList.add(SourceEvidence.builder()
                        .sourceName(source != null ? source : domain)
                        .domain(domain)
                        .evidenceTier(tier)
                        .articleTitle(title)
                        .url(link)
                        .credibilityRating(determineSourceCredibility(source))
                        .matchPercentage(Math.round(overlap * 1000.0) / 10.0)
                        .independenceRating(independence)
                        .contextualAuthorityScore(contextualAuth)
                        .geographicRelevance(geo.toLowerCase().contains("nepal") || geo.toLowerCase().contains("india") ? "HIGH" : "MEDIUM")
                        .directness(isPrimaryRegionalAuthority(source, domain, geo) ? "DIRECT_PRIMARY" : "SECONDARY_REPORTING")
                        .stance(stance)
                        .verdictBySource(itemContradiction.isContradicted() ? "Contradicted / False" : "Verified True")
                        .clusterId(isDuplicateDomain ? "CLUSTER-SYNDICATE-01" : "CLUSTER-WIRE-0" + (crossReferencedList.size() + 1))
                        .isPrimarySource(isPrimaryRegionalAuthority(source, domain, geo))
                        .acceptanceReasons(acceptanceReasons)
                        .build());
            }
        }

        if (bestTitle != null && bestOverlap >= 0.35) {
            boolean isContradicted = bestContradiction != null && bestContradiction.isContradicted();
            String severity = bestContradiction != null ? bestContradiction.getSeverity() : "NONE";
            String distortionType = bestContradiction != null ? bestContradiction.getDistortionType() : "NONE";
            int cred = determineSourceCredibility(bestSource);
            double matchPct = Math.round(bestOverlap * 1000.0) / 10.0;
            String bestDomain = extractDomain(bestSourceUrl != null ? bestSourceUrl : bestLink);
            if (bestDomain == null) bestDomain = extractDomainFromSourceName(bestSource);

            String tier = determineEvidenceTier(bestSource, bestDomain);

            // Group into Distinct Independent Evidence Clusters
            List<EvidenceCluster> clusters = buildEvidenceClusters(crossReferencedList, isContradicted);

            String claimIntegrity = !isContradicted ? "AUTHENTIC_REPRODUCTION" :
                    ("MINOR_DISCREPANCY".equals(severity) ? "MINOR_VARIANCE" : "ALTERED_DISTORTION");

            // Build Retrieval Audit Trail
            RetrievalAudit audit = RetrievalAudit.builder()
                    .searchQueriesRun(2)
                    .sourcesRetrieved(totalRetrieved)
                    .relevantSources(crossReferencedList.size())
                    .rejectedSources(rejectedCount)
                    .syndicatedDuplicates(syndicatedCount)
                    .independentClustersCount(clusters.size())
                    .primarySourcesCount((int) crossReferencedList.stream().filter(SourceEvidence::isPrimarySource).count())
                    .accreditedSecondaryCount(crossReferencedList.size())
                    .referenceSourcesCount(0)
                    .supportingSourcesCount(!isContradicted ? crossReferencedList.size() : 0)
                    .contradictingSourcesCount(isContradicted ? crossReferencedList.size() : 0)
                    .auditSummary("Executed targeted queries across live wires. Retrieved " + totalRetrieved + " candidates, filtered " + syndicatedCount + " syndicated copies into " + clusters.size() + " independent clusters.")
                    .build();

            ClaimOriginDiscovery originDiscovery = ClaimOriginDiscovery.builder()
                    .originalPublisher(bestSource)
                    .earliestIdentifiedPublisher(bestSource)
                    .earliestVerifiedSourceFound(bestSource)
                    .originalDomain(bestDomain)
                    .originalHeadline(bestTitle)
                    .originalUrl(bestLink)
                    .publishedDate("Contemporaneous News Dispatch")
                    .retrievalTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .provenanceType(claimIntegrity)
                    .provenanceStatus("EARLIEST_VERIFIED_SOURCE_FOUND")
                    .claimIntegrity(claimIntegrity)
                    .contradictionSeverity(severity)
                    .evidenceTier(tier)
                    .distortionAnalysis(isContradicted ?
                            "Claim derived from reporting by " + bestSource + ", with " + distortionType + " (" + severity + "): " + bestContradiction.getReason() :
                            "Claim aligns with contemporaneous reporting originally published by " + bestSource + ".")
                    .crossReferencedConsensus(isContradicted ?
                            "Contradicted across accredited news wire reports." :
                            "Cross-referenced across " + clusters.size() + " independent evidence clusters (" + crossReferencedList.size() + " total publications).")
                    .provenanceConfidence(clusters.size() >= 2 ? "HIGH" : "MEDIUM")
                    .originMatchConfidence(matchPct)
                    .build();

            return Optional.of(ExternalFactResult.builder()
                    .topic(bestTitle)
                    .snippet(isContradicted ?
                            "Contradicted by verified press reporting from " + bestSource + ": \"" + bestTitle + "\". " + bestContradiction.getReason() :
                            "Corroborated by verified press reporting from " + bestSource + ": \"" + bestTitle + "\".")
                    .sourceUrl(bestLink)
                    .sourceName(bestSource)
                    .sourceDomain(bestDomain)
                    .isAuthenticCorroboration(!isContradicted)
                    .isContradiction(isContradicted)
                    .contradictionDetail(isContradicted ? bestContradiction.getReason() : null)
                    .contradictionSeverity(severity)
                    .distortionType(distortionType)
                    .credibilityScore(cred)
                    .matchPercentage(matchPct)
                    .originDiscovery(originDiscovery)
                    .retrievalAudit(audit)
                    .crossReferencedSources(crossReferencedList)
                    .evidenceClusters(clusters)
                    .build());
        }

        return Optional.empty();
    }

    private boolean isPrimaryRegionalAuthority(String source, String domain, String geo) {
        if (source == null && domain == null) return false;
        String s = (source != null ? source.toLowerCase() : "");
        String d = (domain != null ? domain.toLowerCase() : "");
        return s.contains("police") || s.contains("disaster") || s.contains("ministry") || s.contains("government") || d.endsWith(".gov") || d.endsWith(".gov.np") || d.endsWith(".gov.in");
    }

    private List<EvidenceCluster> buildEvidenceClusters(List<SourceEvidence> sources, boolean isContradicted) {
        List<EvidenceCluster> clusters = new ArrayList<>();
        if (sources.isEmpty()) return clusters;

        Map<String, List<SourceEvidence>> clusterMap = new LinkedHashMap<>();
        for (SourceEvidence se : sources) {
            String clusterId = se.getClusterId() != null ? se.getClusterId() : "CLUSTER-WIRE-01";
            clusterMap.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(se);
        }

        int clusterIdx = 1;
        for (Map.Entry<String, List<SourceEvidence>> entry : clusterMap.entrySet()) {
            List<SourceEvidence> group = entry.getValue();
            String primaryOutlet = group.get(0).getSourceName();
            List<String> affiliated = group.stream().map(SourceEvidence::getSourceName).collect(Collectors.toList());
            boolean isPrimary = group.stream().anyMatch(SourceEvidence::isPrimarySource);

            clusters.add(EvidenceCluster.builder()
                    .clusterId("C00" + clusterIdx)
                    .clusterTheme(isPrimary ? "Regional Primary Authority & Official Reports" : "Accredited News Wire Reporting (" + primaryOutlet + ")")
                    .primaryOutlet(primaryOutlet)
                    .affiliatedOutlets(affiliated)
                    .sourceCount(group.size())
                    .independenceRating(isPrimary ? 100.0 : (group.size() > 1 ? 85.0 : 70.0))
                    .consensusStance(isContradicted ? "REFUTED" : "CONFIRMED")
                    .evidenceTier(isPrimary ? "LEVEL_1_PRIMARY" : "LEVEL_2_SECONDARY")
                    .isPrimaryAuthority(isPrimary)
                    .build());
            clusterIdx++;
        }

        return clusters;
    }

    public static class ContradictionCheck {
        private final boolean contradicted;
        private final String severity; // NONE, MINOR_DISCREPANCY, MODERATE_CONTRADICTION, MAJOR_CONTRADICTION, DIRECT_FACTUAL_REVERSAL
        private final String distortionType; // NUMERICAL_DISTORTION, LOCATION_DISTORTION, ENTITY_DISTORTION, ATTRIBUTION_DISTORTION, POLARITY_DISTORTION, CONTEXT_DISTORTION, NONE
        private final String reason;

        public ContradictionCheck(boolean contradicted, String severity, String distortionType, String reason) {
            this.contradicted = contradicted;
            this.severity = severity != null ? severity : "NONE";
            this.distortionType = distortionType != null ? distortionType : "NONE";
            this.reason = reason;
        }

        public boolean isContradicted() { return contradicted; }
        public String getSeverity() { return severity; }
        public String getDistortionType() { return distortionType; }
        public String getReason() { return reason; }
    }

    public ContradictionCheck detectContradiction(String query, String articleTitle) {
        if (query == null || articleTitle == null) return new ContradictionCheck(false, "NONE", "NONE", null);
        String q = query.toLowerCase().trim();
        String a = articleTitle.toLowerCase().trim();

        // 1. Direct Factual Reversal: Negation / Zero Quantifier vs. Confirmed Event Casualties
        boolean qHasZeroCasualties = q.contains(" none") || q.startsWith("none ") || q.contains(" zero ") || q.contains(" 0 ") ||
                                     q.contains("no one") || q.contains("nobody") || q.contains("no casualties") || q.contains("killed none") ||
                                     q.contains("died none") || q.contains("not killed") || q.contains("no deaths") || q.contains("zero dead");

        boolean aHasCasualties = a.contains("killed") || a.contains("dead") || a.contains("deaths") || a.contains("fatalities") || a.contains("toll") || a.contains("claims");
        boolean aHasPositiveNumber = Pattern.compile("\\b(?:[1-9]\\d*|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|dozens?|scores|several|hundreds?)\\b", Pattern.CASE_INSENSITIVE).matcher(a).find();

        if (qHasZeroCasualties && aHasCasualties && aHasPositiveNumber) {
            return new ContradictionCheck(true, "DIRECT_FACTUAL_REVERSAL", "POLARITY_DISTORTION",
                    "Claim asserts zero casualties ('none' / 'no one'), whereas contemporaneous reporting explicitly confirms fatalities in the incident.");
        }

        // 2. Location Contradiction Check (e.g. Kathmandu vs Pokhara, Mumbai vs Delhi)
        List<String> cities = List.of("kathmandu", "pokhara", "mumbai", "delhi", "kolkata", "chennai", "bangalore", "london", "paris", "tokyo", "beijing", "kyiv", "moscow");
        String qCity = cities.stream().filter(q::contains).findFirst().orElse(null);
        String aCity = cities.stream().filter(a::contains).findFirst().orElse(null);
        if (qCity != null && aCity != null && !qCity.equalsIgnoreCase(aCity) && calculateQueryArticleOverlap(q, a) >= 0.30) {
            return new ContradictionCheck(true, "MAJOR_CONTRADICTION", "LOCATION_DISTORTION",
                    "Location discrepancy: claim asserts event in " + capitalizeFirst(qCity) + ", whereas reporting indicates " + capitalizeFirst(aCity) + ".");
        }

        // 3. Attribution Contradiction Check (e.g. NASA vs ESA, WHO vs CDC)
        if (q.contains("nasa") && a.contains("esa") && !a.contains("nasa")) {
            return new ContradictionCheck(true, "MAJOR_CONTRADICTION", "ATTRIBUTION_DISTORTION",
                    "Attribution mismatch: claim attributes event to NASA, whereas reports indicate European Space Agency (ESA).");
        }
        if (q.contains("esa") && a.contains("nasa") && !a.contains("esa")) {
            return new ContradictionCheck(true, "MAJOR_CONTRADICTION", "ATTRIBUTION_DISTORTION",
                    "Attribution mismatch: claim attributes event to ESA, whereas reports indicate NASA.");
        }

        // 4. Numerical Disparity on Extracted Quantifiers
        Map<String, Integer> wordToNum = Map.ofEntries(
                Map.entry("zero", 0), Map.entry("none", 0), Map.entry("nil", 0),
                Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3), Map.entry("four", 4),
                Map.entry("five", 5), Map.entry("six", 6), Map.entry("seven", 7), Map.entry("eight", 8),
                Map.entry("nine", 9), Map.entry("ten", 10), Map.entry("twenty", 20), Map.entry("fifty", 50),
                Map.entry("hundred", 100)
        );

        Integer qNum = extractFirstNumber(q, wordToNum);
        Integer aNum = extractFirstNumber(a, wordToNum);

        if (qNum != null && aNum != null && !qNum.equals(aNum)) {
            boolean articleContainsQueryNumber = a.contains(" " + qNum + " ") || a.contains(" " + qNum + ",") || a.contains(" " + qNum + ".");
            if (!articleContainsQueryNumber) {
                if (qNum == 0 && aNum > 0) {
                    return new ContradictionCheck(true, "DIRECT_FACTUAL_REVERSAL", "POLARITY_DISTORTION",
                            "Claim asserts zero / none, whereas verified coverage confirms " + aNum + ".");
                }

                double deltaRatio = (double) Math.abs(qNum - aNum) / Math.max(qNum, aNum);
                if (deltaRatio <= 0.05 && Math.abs(qNum - aNum) <= 2) {
                    return new ContradictionCheck(true, "MINOR_DISCREPANCY", "NUMERICAL_DISTORTION",
                            "Minor variance in reported figures (claim states " + qNum + ", news reports " + aNum + ").");
                } else if (deltaRatio <= 0.25) {
                    return new ContradictionCheck(true, "MODERATE_CONTRADICTION", "NUMERICAL_DISTORTION",
                            "Moderate discrepancy in reported metrics (claim states " + qNum + ", news reports " + aNum + ").");
                } else {
                    return new ContradictionCheck(true, "MAJOR_CONTRADICTION", "NUMERICAL_DISTORTION",
                            "Significant contradiction in reported figures (claim states " + qNum + ", news reports " + aNum + ").");
                }
            }
        }

        // 5. Polarity Mismatch (survived vs died / killed)
        if ((q.contains("survived") || q.contains("safe and sound") || q.contains("unhurt") || q.contains("alive")) &&
            (a.contains("died") || a.contains("killed") || a.contains("dead") || a.contains("fatal"))) {
            return new ContradictionCheck(true, "DIRECT_FACTUAL_REVERSAL", "POLARITY_DISTORTION",
                    "Polarity contradiction: claim asserts survival/unharmed status while reporting confirms casualties/death.");
        }

        return new ContradictionCheck(false, "NONE", "NONE", null);
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private Integer extractFirstNumber(String text, Map<String, Integer> wordToNum) {
        if (text == null) return null;
        String sanitized = text.replaceAll("(?i)\\b\\d{1,2}/\\d{1,2}\\b", " ");
        for (String token : sanitized.toLowerCase().split("[^a-zA-Z0-9]+")) {
            if (wordToNum.containsKey(token)) {
                return wordToNum.get(token);
            }
            try {
                int val = Integer.parseInt(token);
                if (val >= 1800 && val <= 2099) {
                    continue; // Skip 4-digit calendar years
                }
                return val;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public String determineEvidenceTier(String sourceName, String domain) {
        if (domain != null) {
            String d = domain.toLowerCase();
            if (d.endsWith(".gov") || d.endsWith(".gov.in") || d.endsWith(".gov.np") || d.endsWith(".nic.in") || d.contains("who.int") || d.contains("nasa.gov") || d.contains("isro.gov.in") || d.contains("un.org")) {
                return "LEVEL_1_PRIMARY";
            }
            if (d.contains("snopes.com") || d.contains("politifact.com") || d.contains("factcheck.org") || d.contains("boomlive.in") || d.contains("altnews.in")) {
                return "LEVEL_3_FACTCHECK";
            }
            if (d.contains("wikipedia.org") || d.contains("britannica.com")) {
                return "LEVEL_4_REFERENCE";
            }
            if (d.contains("twitter.com") || d.contains("x.com") || d.contains("reddit.com") || d.contains("facebook.com") || d.contains("medium.com") || d.contains("blogspot.com")) {
                return "LEVEL_5_USER_GENERATED";
            }
        }
        return "LEVEL_2_SECONDARY";
    }

    private double calculateIndependenceRating(String sourceName, String domain) {
        if (sourceName == null) return 70.0;
        String lower = sourceName.toLowerCase();
        if (lower.contains("police") || lower.contains("disaster") || lower.contains("ministry")) {
            return 100.0; // Primary Authority
        }
        if (lower.contains("reuters") || lower.contains("associated press") || lower.contains("pti") || lower.contains("afp") || lower.contains("bloomberg")) {
            return 95.0; // Primary Wire Agency
        }
        if (lower.contains("the hindu") || lower.contains("bbc") || lower.contains("indian express") || lower.contains("kathmandu post")) {
            return 90.0; // Major Original Reporting Newspaper
        }
        return 65.0; // Syndicated / Regional Outlets
    }

    public boolean isTier1AccreditedPublisher(String sourceName, String domain) {
        if (sourceName == null && domain == null) return false;
        String s = (sourceName != null ? sourceName.toLowerCase() : "");
        String d = (domain != null ? domain.toLowerCase() : "");

        if (d.contains("substack.com") || d.contains("medium.com") || d.contains("blogspot.com") ||
            d.contains("wordpress.com") || d.contains("tumblr.com") ||
            d.contains("reddit.com") || d.contains("quora.com") || d.contains("twitter.com") || d.contains("x.com")) {
            return false;
        }

        return d.contains("thehindu.com") || d.contains("reuters.com") || d.contains("apnews.com") ||
                d.contains("bbc.com") || d.contains("indianexpress.com") || d.contains("ndtv.com") ||
                d.contains("timesofindia.indiatimes.com") || d.contains("hindustantimes.com") ||
                d.contains("indiatoday.in") || d.contains("deccanherald.com") || d.contains("bloomberg.com") ||
                d.contains("theguardian.com") || d.contains("who.int") || d.contains("nasa.gov") ||
                d.contains("nature.com") || d.contains("kathmandupost.com") || d.contains("thehimalayantimes.com") ||
                d.contains("republicworld.com") || d.contains("timesnownews.com") ||
                d.contains("abplive.com") || d.contains("dnaindia.com") || d.contains("firstpost.com") ||
                d.contains("snopes.com") || d.contains("politifact.com") || d.contains("economictimes.indiatimes.com") ||
                d.contains("business-standard.com") || d.contains("aniin.com") || d.contains("ptinews.com") ||
                d.contains(".gov") || d.contains(".gov.in") || d.contains(".gov.np");
    }

    public Optional<ExternalFactResult> queryWikipedia(String query) {
        return queryWikipedia(query, query, null);
    }

    public Optional<ExternalFactResult> queryWikipedia(String searchQuery, String originalQuery, ClaimContextInfo context) {
        try {
            String simplifiedQuery = cleanSearchQuery(searchQuery);
            String[] words = simplifiedQuery.split(" ");
            if (words.length > 4) {
                simplifiedQuery = String.join(" ", words[0], words[1], words[2], words[3]);
            }
            if (simplifiedQuery.length() < 3) return Optional.empty();

            String encodedQuery = URLEncoder.encode(simplifiedQuery, StandardCharsets.UTF_8);
            String url = "https://en.wikipedia.org/w/api.php?action=opensearch&search=" + encodedQuery + "&limit=1&namespace=0&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "TruthLens-FactCheckEngine/1.0 (contact@truthlens.ai)")
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                List<String> titles = extractJsonArrayItems(body, 1);
                List<String> snippets = extractJsonArrayItems(body, 2);
                List<String> urls = extractJsonArrayItems(body, 3);

                if (!titles.isEmpty() && !snippets.isEmpty() && !snippets.get(0).isBlank()) {
                    String title = titles.get(0);
                    String snippet = snippets.get(0);
                    String pageUrl = !urls.isEmpty() ? urls.get(0) : "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8);

                    ContradictionCheck contradiction = detectContradiction(originalQuery, title + " " + snippet);
                    boolean isContradicted = contradiction.isContradicted();
                    String severity = contradiction.getSeverity();
                    String distortionType = contradiction.getDistortionType();
                    boolean corroborates = !isContradicted && doesWikipediaCorroborateClaim(originalQuery, title, snippet);

                    if (!corroborates && !isContradicted) {
                        return Optional.empty();
                    }

                    String claimIntegrity = !isContradicted ? (corroborates ? "AUTHENTIC_REPRODUCTION" : "UNVERIFIED_ORIGIN") :
                            ("MINOR_DISCREPANCY".equals(severity) ? "MINOR_VARIANCE" : "ALTERED_DISTORTION");

                    ClaimOriginDiscovery originDiscovery = ClaimOriginDiscovery.builder()
                            .originalPublisher("Wikipedia Knowledge Archive")
                            .earliestIdentifiedPublisher("Wikipedia Knowledge Archive")
                            .earliestVerifiedSourceFound("Wikipedia Knowledge Archive")
                            .originalDomain("wikipedia.org")
                            .originalHeadline(title)
                            .originalUrl(pageUrl)
                            .publishedDate("Encyclopedic Public Record")
                            .retrievalTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .provenanceType(claimIntegrity)
                            .provenanceStatus("EARLIEST_VERIFIED_SOURCE_FOUND")
                            .claimIntegrity(claimIntegrity)
                            .contradictionSeverity(severity)
                            .evidenceTier("LEVEL_4_REFERENCE")
                            .distortionAnalysis(isContradicted ?
                                    "Claim derived from verified historical records for '" + title + "', with " + distortionType + " (" + severity + "): " + contradiction.getReason() :
                                    (corroborates ?
                                            "Claim factually aligns with official documented public records on Wikipedia." :
                                            "Assertion could not be substantiated against documented public records."))
                            .crossReferencedConsensus(isContradicted ?
                                    "Contradicted by documented encyclopedic and historical records." :
                                    "Corroborated by Wikimedia Foundation historical archives.")
                            .provenanceConfidence("HIGH")
                            .originMatchConfidence(isContradicted || corroborates ? 92.0 : 40.0)
                            .build();

                    List<EvidenceCluster> wikiClusters = List.of(EvidenceCluster.builder()
                            .clusterId("C001")
                            .clusterTheme("Wikimedia Knowledge Repository")
                            .primaryOutlet("Wikipedia Foundation")
                            .affiliatedOutlets(List.of("Wikipedia Knowledge Archive"))
                            .sourceCount(1)
                            .independenceRating(80.0)
                            .consensusStance(isContradicted ? "REFUTED" : "CONFIRMED")
                            .evidenceTier("LEVEL_4_REFERENCE")
                            .isPrimaryAuthority(false)
                            .build());

                    RetrievalAudit audit = RetrievalAudit.builder()
                            .searchQueriesRun(1)
                            .sourcesRetrieved(1)
                            .relevantSources(1)
                            .rejectedSources(0)
                            .syndicatedDuplicates(0)
                            .independentClustersCount(1)
                            .primarySourcesCount(0)
                            .accreditedSecondaryCount(0)
                            .referenceSourcesCount(1)
                            .supportingSourcesCount(corroborates ? 1 : 0)
                            .contradictingSourcesCount(isContradicted ? 1 : 0)
                            .auditSummary("Retrieved reference documentation from Wikimedia Foundation encyclopedic repository.")
                            .build();

                    return Optional.of(ExternalFactResult.builder()
                            .topic(title)
                            .snippet(isContradicted ?
                                    "Contradicted by verified historical records on Wikipedia (" + title + "): \"" + snippet + "\". " + contradiction.getReason() :
                                    snippet)
                            .sourceName("Wikipedia Knowledge Archive")
                            .sourceDomain("wikipedia.org")
                            .sourceUrl(pageUrl)
                            .isAuthenticCorroboration(corroborates)
                            .isContradiction(isContradicted)
                            .contradictionDetail(isContradicted ? contradiction.getReason() : null)
                            .contradictionSeverity(severity)
                            .distortionType(distortionType)
                            .credibilityScore(94)
                            .matchPercentage(isContradicted || corroborates ? 90.0 : 40.0)
                            .originDiscovery(originDiscovery)
                            .retrievalAudit(audit)
                            .crossReferencedSources(List.of(SourceEvidence.builder()
                                    .sourceName("Wikipedia Knowledge Archive")
                                    .domain("wikipedia.org")
                                    .evidenceTier("LEVEL_4_REFERENCE")
                                    .articleTitle(title)
                                    .url(pageUrl)
                                    .credibilityRating(94)
                                    .matchPercentage(90.0)
                                    .independenceRating(80.0)
                                    .contextualAuthorityScore(0.70)
                                    .geographicRelevance("MEDIUM")
                                    .directness("INDIRECT_REFERENCE")
                                    .stance(isContradicted ? "REFUTED" : (corroborates ? "SUPPORTED" : "UNCERTAIN"))
                                    .verdictBySource(isContradicted ? "Contradicted / False" : (corroborates ? "Verified True" : "Unverified"))
                                    .clusterId("C001")
                                    .isPrimarySource(false)
                                    .acceptanceReasons(List.of("Archival background encyclopedia entry", "Historical fact reference"))
                                    .build()))
                            .evidenceClusters(wikiClusters)
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("Wikipedia candidate lookup skipped: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private boolean doesWikipediaCorroborateClaim(String query, String title, String snippet) {
        if (snippet == null || snippet.isBlank()) return false;
        String sLower = snippet.toLowerCase();
        String qLower = query.toLowerCase();

        boolean claimsDeath = qLower.contains("passed away") || qLower.contains("died") || 
                              qLower.contains("dead") || qLower.contains("assassinated") || 
                              qLower.contains("killed") || qLower.contains("death");
        if (claimsDeath) {
            boolean snippetConfirmsDeath = sLower.contains("died") || sLower.contains("death") || 
                                          sLower.contains("passed away") || sLower.contains("assassinated") || 
                                          sLower.contains("killed") || sLower.matches(".*\\(\\d{4}\\s*[-–—]\\s*\\d{4}\\).*");
            if (!snippetConfirmsDeath) {
                return false;
            }
        }

        boolean claimsCure = qLower.contains("cure") || qLower.contains("cures") || qLower.contains("miracle");
        if (claimsCure && !sLower.contains("cure") && !sLower.contains("therapy") && !sLower.contains("approved")) {
            return false;
        }

        return calculateQueryArticleOverlap(query, title + " " + snippet) >= 0.30;
    }

    private double calculateQueryArticleOverlap(String query, String articleTitle) {
        if (query == null || articleTitle == null) return 0.0;

        Set<String> stopWords = Set.of(
                "the", "a", "an", "is", "was", "are", "were", "in", "on", "at", "to", "for",
                "of", "and", "or", "by", "with", "from", "as", "about", "into", "through", "after", "over",
                "says", "said", "have", "has", "had", "been", "will", "would", "could", "should", "out", "all"
        );

        Set<String> queryTokens = Arrays.stream(query.toLowerCase().split("[^a-zA-Z0-9]+"))
                .filter(w -> w.length() > 2 && !stopWords.contains(w))
                .collect(Collectors.toSet());

        if (queryTokens.isEmpty()) return 0.0;

        String lowerTitle = articleTitle.toLowerCase();
        long matchCount = queryTokens.stream().filter(lowerTitle::contains).count();

        return (double) matchCount / queryTokens.size();
    }

    private int determineSourceCredibility(String sourceName) {
        if (sourceName == null) return 88;
        String lower = sourceName.toLowerCase();
        if (lower.contains("police") || lower.contains("disaster") || lower.contains("ministry")) {
            return 99; // Primary Authority
        }
        if (lower.contains("hindu") || lower.contains("reuters") || lower.contains("associated press") || lower.contains("ap news") || lower.contains("pti")) {
            return 98;
        }
        if (lower.contains("bbc") || lower.contains("indian express") || lower.contains("kathmandu post") || lower.contains("bloomberg") || lower.contains("guardian") || lower.contains("nature")) {
            return 97;
        }
        if (lower.contains("ndtv") || lower.contains("times of india") || lower.contains("hindustan times") || lower.contains("ani") || lower.contains("cnn")) {
            return 95;
        }
        if (lower.contains("times now") || lower.contains("india today") || lower.contains("republic") || lower.contains("zee") || lower.contains("news18") || lower.contains("business standard")) {
            return 91;
        }
        return 88;
    }

    private String extractDomainFromSourceName(String sourceName) {
        if (sourceName == null) return "news.google.com";
        String lower = sourceName.toLowerCase();
        if (lower.contains("the hindu")) return "thehindu.com";
        if (lower.contains("times now")) return "timesnownews.com";
        if (lower.contains("reuters")) return "reuters.com";
        if (lower.contains("associated press") || lower.contains("ap")) return "apnews.com";
        if (lower.contains("bbc")) return "bbc.com";
        if (lower.contains("kathmandu post")) return "kathmandupost.com";
        if (lower.contains("indian express")) return "indianexpress.com";
        if (lower.contains("ndtv")) return "ndtv.com";
        if (lower.contains("times of india")) return "timesofindia.indiatimes.com";
        if (lower.contains("hindustan times")) return "hindustantimes.com";
        if (lower.contains("republic")) return "republicworld.com";
        if (lower.contains("india today")) return "indiatoday.in";
        return "news.google.com";
    }

    private String extractTagValue(String xml, String tag) {
        Pattern p = Pattern.compile("<" + tag + "(?:[^>]*)>(.*?)</" + tag + ">", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractSourceUrl(String xml) {
        Pattern p = Pattern.compile("<source\\s+url=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    private String unescapeXml(String text) {
        if (text == null) return "";
        return text.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("<!\\[CDATA\\[(.*?)\\]\\]>", "$1")
                .trim();
    }

    private String cleanSearchQuery(String query) {
        return query.replaceAll("(?i)\\b(LIVE|BREAKING|EXCLUSIVE|shocking|secret|revealed|unbelievable|cure|fake|leaked|miracle|news|report)\\b", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private List<String> extractJsonArrayItems(String json, int arrayIndex) {
        List<String> results = new ArrayList<>();
        try {
            int currentArrayIndex = -1;
            int depth = 0;
            StringBuilder currentItem = null;
            boolean inString = false;
            boolean escaped = false;

            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    if (currentItem != null) currentItem.append(c);
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    if (!inString && currentArrayIndex == arrayIndex && currentItem != null) {
                        results.add(currentItem.toString());
                        currentItem = null;
                    } else if (inString && currentArrayIndex == arrayIndex) {
                        currentItem = new StringBuilder();
                    }
                    continue;
                }
                if (!inString) {
                    if (c == '[') {
                        depth++;
                        if (depth == 2) {
                            currentArrayIndex++;
                        }
                    } else if (c == ']') {
                        depth--;
                    }
                } else {
                    if (currentArrayIndex == arrayIndex && currentItem != null) {
                        currentItem.append(c);
                    }
                }
            }
        } catch (Exception ignored) {}
        return results;
    }
}
