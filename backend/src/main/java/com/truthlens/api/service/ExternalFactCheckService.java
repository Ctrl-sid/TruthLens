package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimVerificationResponse.ClaimOriginDiscovery;
import com.truthlens.api.dto.ClaimVerificationResponse.EvidenceCluster;
import com.truthlens.api.dto.ClaimVerificationResponse.SourceEvidence;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
        private int credibilityScore;
        private double matchPercentage;
        private ClaimOriginDiscovery originDiscovery;
        @Builder.Default
        private List<SourceEvidence> crossReferencedSources = new ArrayList<>();
        @Builder.Default
        private List<EvidenceCluster> evidenceClusters = new ArrayList<>();
    }

    public Optional<ExternalFactResult> queryExternalKnowledge(String query) {
        if (query == null || query.isBlank()) return Optional.empty();

        // 1. Direct Live News Wire Search
        Optional<ExternalFactResult> liveNewsMatch = queryLiveNewsWires(query, query);
        if (liveNewsMatch.isPresent()) {
            return liveNewsMatch;
        }

        // 2. Direct Wikipedia OpenSearch (Level 4 Reference)
        Optional<ExternalFactResult> wikiMatch = queryWikipedia(query, query);
        if (wikiMatch.isPresent()) {
            return wikiMatch;
        }

        // 3. Fallback: If claim contains zero-quantifiers or modifiers, search for base event
        String coreEventQuery = extractCoreEventQuery(query);
        if (!coreEventQuery.equalsIgnoreCase(query) && coreEventQuery.length() >= 4) {
            Optional<ExternalFactResult> liveEventMatch = queryLiveNewsWires(coreEventQuery, query);
            if (liveEventMatch.isPresent()) {
                return liveEventMatch;
            }

            Optional<ExternalFactResult> wikiEventMatch = queryWikipedia(coreEventQuery, query);
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
        return queryLiveNewsWires(query, query);
    }

    public Optional<ExternalFactResult> queryLiveNewsWires(String searchQuery, String originalQuery) {
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
                return parseGoogleNewsRss(response.body(), originalQuery);
            }
        } catch (Exception e) {
            log.debug("Live news wire search skipped or timed out: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<ExternalFactResult> parseGoogleNewsRss(String xml, String originalQuery) {
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

        while (itemMatcher.find()) {
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

            if (title == null || title.isBlank()) continue;

            String domain = extractDomain(sourceUrl != null ? sourceUrl : link);
            if (domain == null) {
                domain = extractDomainFromSourceName(source);
            }

            // Retrieval quality filtering: Check token overlap against query
            double overlap = calculateQueryArticleOverlap(originalQuery, title);
            if (overlap < 0.25) {
                // Skip completely irrelevant search results
                continue;
            }

            if (!isTier1AccreditedPublisher(source, domain)) {
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

            if (domain != null && !seenDomains.contains(domain.toLowerCase()) && crossReferencedList.size() < 6) {
                seenDomains.add(domain.toLowerCase());

                String tier = determineEvidenceTier(source, domain);
                String stance = itemContradiction.isContradicted() ? "REFUTED" : "SUPPORTED";
                double independence = calculateIndependenceRating(source, domain);

                crossReferencedList.add(SourceEvidence.builder()
                        .sourceName(source != null ? source : domain)
                        .domain(domain)
                        .evidenceTier(tier)
                        .articleTitle(title)
                        .url(link)
                        .credibilityRating(determineSourceCredibility(source))
                        .matchPercentage(Math.round(overlap * 1000.0) / 10.0)
                        .independenceRating(independence)
                        .stance(stance)
                        .verdictBySource(itemContradiction.isContradicted() ? "Contradicted / False" : "Verified True")
                        .clusterId("CLUSTER-WIRE-01")
                        .build());
            }
        }

        if (bestTitle != null && bestOverlap >= 0.35) {
            boolean isContradicted = bestContradiction != null && bestContradiction.isContradicted();
            int cred = determineSourceCredibility(bestSource);
            double matchPct = Math.round(bestOverlap * 1000.0) / 10.0;
            String bestDomain = extractDomain(bestSourceUrl != null ? bestSourceUrl : bestLink);
            if (bestDomain == null) bestDomain = extractDomainFromSourceName(bestSource);

            String tier = determineEvidenceTier(bestSource, bestDomain);

            // Group into Evidence Clusters
            List<EvidenceCluster> clusters = buildEvidenceClusters(crossReferencedList, isContradicted);

            ClaimOriginDiscovery originDiscovery = ClaimOriginDiscovery.builder()
                    .originalPublisher(bestSource)
                    .originalDomain(bestDomain)
                    .originalHeadline(bestTitle)
                    .originalUrl(bestLink)
                    .publishedDate("Verified Press Dispatch")
                    .provenanceType(isContradicted ? "ALTERED_DISTORTION" : "AUTHENTIC_REPRODUCTION")
                    .provenanceStatus(crossReferencedList.size() > 1 ? "MULTIPLE_RELATED_SOURCES_FOUND" : "ORIGINAL_REPORT_FOUND")
                    .evidenceTier(tier)
                    .distortionAnalysis(isContradicted ?
                            "Claim derived from a verified news event reported by " + bestSource + ", but altered: " + bestContradiction.getReason() :
                            "Claim faithfully matches verified reporting originally published by " + bestSource + ".")
                    .crossReferencedConsensus(isContradicted ?
                            "Contradicted across accredited news wire reports." :
                            "Cross-referenced and corroborated across " + crossReferencedList.size() + " accredited news wire agencies.")
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
                    .credibilityScore(cred)
                    .matchPercentage(matchPct)
                    .originDiscovery(originDiscovery)
                    .crossReferencedSources(crossReferencedList)
                    .evidenceClusters(clusters)
                    .build());
        }

        return Optional.empty();
    }

    private List<EvidenceCluster> buildEvidenceClusters(List<SourceEvidence> sources, boolean isContradicted) {
        List<EvidenceCluster> clusters = new ArrayList<>();
        if (sources.isEmpty()) return clusters;

        List<String> affiliated = sources.stream()
                .map(SourceEvidence::getSourceName)
                .collect(Collectors.toList());

        String primaryOutlet = sources.get(0).getSourceName();

        clusters.add(EvidenceCluster.builder()
                .clusterId("CLUSTER-WIRE-01")
                .clusterTheme("Tier-1 Mainstream Wire & Press Reporting")
                .primaryOutlet(primaryOutlet)
                .affiliatedOutlets(affiliated)
                .sourceCount(sources.size())
                .independenceRating(sources.size() >= 3 ? 90.0 : 70.0)
                .consensusStance(isContradicted ? "CONTRADICTED" : "SUPPORTED")
                .evidenceTier("LEVEL_2_SECONDARY")
                .build());

        return clusters;
    }

    public static class ContradictionCheck {
        private final boolean contradicted;
        private final String reason;

        public ContradictionCheck(boolean contradicted, String reason) {
            this.contradicted = contradicted;
            this.reason = reason;
        }

        public boolean isContradicted() { return contradicted; }
        public String getReason() { return reason; }
    }

    public ContradictionCheck detectContradiction(String query, String articleTitle) {
        if (query == null || articleTitle == null) return new ContradictionCheck(false, null);
        String q = query.toLowerCase().trim();
        String a = articleTitle.toLowerCase().trim();

        // 1. Negation / Zero Quantifier vs. Confirmed Event Casualties
        boolean qHasZeroCasualties = q.contains(" none") || q.startsWith("none ") || q.contains(" zero ") || q.contains(" 0 ") ||
                                     q.contains("no one") || q.contains("nobody") || q.contains("no casualties") || q.contains("killed none") ||
                                     q.contains("died none") || q.contains("not killed") || q.contains("no deaths") || q.contains("zero dead");

        boolean aHasCasualties = a.contains("killed") || a.contains("dead") || a.contains("deaths") || a.contains("fatalities") || a.contains("toll") || a.contains("claims");
        boolean aHasPositiveNumber = Pattern.compile("\\b(?:[1-9]\\d*|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|dozens?|scores|several|hundreds?)\\b", Pattern.CASE_INSENSITIVE).matcher(a).find();

        if (qHasZeroCasualties && aHasCasualties && aHasPositiveNumber) {
            return new ContradictionCheck(true, "Claim asserts zero casualties ('none' / 'no one'), whereas verified reporting explicitly confirms fatalities in the incident.");
        }

        // 2. Numerical Disparity on Extracted Quantifiers
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
                    return new ContradictionCheck(true, "Claim asserts zero / none, whereas verified coverage confirms " + aNum + ".");
                } else if (Math.abs(qNum - aNum) >= 3 && Math.max(qNum, aNum) >= 5) {
                    return new ContradictionCheck(true, "Significant discrepancy in reported figures (claim states " + qNum + ", news reports " + aNum + ").");
                }
            }
        }

        // 3. Polarity Mismatch (survived vs died / killed)
        if ((q.contains("survived") || q.contains("safe and sound") || q.contains("unhurt") || q.contains("alive")) &&
            (a.contains("died") || a.contains("killed") || a.contains("dead") || a.contains("fatal"))) {
            return new ContradictionCheck(true, "Polarity contradiction: claim asserts survival/unharmed status while reporting confirms casualties/death.");
        }

        return new ContradictionCheck(false, null);
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
            if (d.endsWith(".gov") || d.endsWith(".gov.in") || d.endsWith(".nic.in") || d.contains("who.int") || d.contains("nasa.gov") || d.contains("isro.gov.in") || d.contains("un.org")) {
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
        if (lower.contains("reuters") || lower.contains("associated press") || lower.contains("pti") || lower.contains("afp") || lower.contains("bloomberg")) {
            return 100.0; // Primary Wire Agency
        }
        if (lower.contains("the hindu") || lower.contains("bbc") || lower.contains("indian express") || lower.contains("nature")) {
            return 85.0; // Major Original Reporting Newspaper
        }
        return 65.0; // Syndicated / Regional Outlets
    }

    public boolean isTier1AccreditedPublisher(String sourceName, String domain) {
        if (sourceName == null && domain == null) return false;
        String s = (sourceName != null ? sourceName.toLowerCase() : "");
        String d = (domain != null ? domain.toLowerCase() : "");

        if (d.contains("substack.com") || d.contains("medium.com") || d.contains("blogspot.com") ||
            d.contains("wordpress.com") || d.contains("tumblr.com") || d.contains("tomaspueyo.com") ||
            d.contains("reddit.com") || d.contains("quora.com") || d.contains("twitter.com") || d.contains("x.com")) {
            return false;
        }

        return d.contains("thehindu.com") || d.contains("reuters.com") || d.contains("apnews.com") ||
                d.contains("bbc.com") || d.contains("indianexpress.com") || d.contains("ndtv.com") ||
                d.contains("timesofindia.indiatimes.com") || d.contains("hindustantimes.com") ||
                d.contains("indiatoday.in") || d.contains("deccanherald.com") || d.contains("bloomberg.com") ||
                d.contains("theguardian.com") || d.contains("who.int") || d.contains("nasa.gov") ||
                d.contains("nature.com") || d.contains("republicworld.com") || d.contains("timesnownews.com") ||
                d.contains("abplive.com") || d.contains("dnaindia.com") || d.contains("firstpost.com") ||
                d.contains("snopes.com") || d.contains("politifact.com") || d.contains("economictimes.indiatimes.com") ||
                d.contains("business-standard.com") || d.contains("aniin.com") || d.contains("ptinews.com");
    }

    public Optional<ExternalFactResult> queryWikipedia(String query) {
        return queryWikipedia(query, query);
    }

    public Optional<ExternalFactResult> queryWikipedia(String searchQuery, String originalQuery) {
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
                    boolean corroborates = !isContradicted && doesWikipediaCorroborateClaim(originalQuery, title, snippet);

                    if (!corroborates && !isContradicted) {
                        return Optional.empty();
                    }

                    ClaimOriginDiscovery originDiscovery = ClaimOriginDiscovery.builder()
                            .originalPublisher("Wikipedia Knowledge Archive")
                            .originalDomain("wikipedia.org")
                            .originalHeadline(title)
                            .originalUrl(pageUrl)
                            .publishedDate("Encyclopedic Public Record")
                            .provenanceType(isContradicted ? "ALTERED_DISTORTION" : (corroborates ? "AUTHENTIC_REPRODUCTION" : "UNVERIFIED_ORIGIN"))
                            .provenanceStatus("SECONDARY_REPORT_FOUND")
                            .evidenceTier("LEVEL_4_REFERENCE")
                            .distortionAnalysis(isContradicted ?
                                    "Claim derived from verified historical records for '" + title + "', but key facts were distorted: " + contradiction.getReason() :
                                    (corroborates ?
                                            "Claim factually aligns with official documented public and historical records on Wikipedia." :
                                            "Assertion could not be substantiated against documented public records."))
                            .crossReferencedConsensus(isContradicted ?
                                    "Contradicted by documented encyclopedic and historical records." :
                                    "Corroborated by Wikimedia Foundation historical archives.")
                            .originMatchConfidence(isContradicted || corroborates ? 92.0 : 40.0)
                            .build();

                    List<EvidenceCluster> wikiClusters = List.of(EvidenceCluster.builder()
                            .clusterId("CLUSTER-REF-01")
                            .clusterTheme("Wikimedia Knowledge Repository")
                            .primaryOutlet("Wikipedia Foundation")
                            .affiliatedOutlets(List.of("Wikipedia Knowledge Archive"))
                            .sourceCount(1)
                            .independenceRating(80.0)
                            .consensusStance(isContradicted ? "CONTRADICTED" : "SUPPORTED")
                            .evidenceTier("LEVEL_4_REFERENCE")
                            .build());

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
                            .credibilityScore(94)
                            .matchPercentage(isContradicted || corroborates ? 90.0 : 40.0)
                            .originDiscovery(originDiscovery)
                            .crossReferencedSources(List.of(SourceEvidence.builder()
                                    .sourceName("Wikipedia Knowledge Archive")
                                    .domain("wikipedia.org")
                                    .evidenceTier("LEVEL_4_REFERENCE")
                                    .articleTitle(title)
                                    .url(pageUrl)
                                    .credibilityRating(94)
                                    .matchPercentage(90.0)
                                    .independenceRating(80.0)
                                    .stance(isContradicted ? "REFUTED" : (corroborates ? "SUPPORTED" : "UNCERTAIN"))
                                    .verdictBySource(isContradicted ? "Contradicted / False" : (corroborates ? "Verified True" : "Unverified"))
                                    .clusterId("CLUSTER-REF-01")
                                    .build()))
                            .evidenceClusters(wikiClusters)
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("Wikipedia knowledge lookup skipped: {}", e.getMessage());
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
        if (lower.contains("hindu") || lower.contains("reuters") || lower.contains("associated press") || lower.contains("ap news") || lower.contains("pti")) {
            return 98;
        }
        if (lower.contains("bbc") || lower.contains("indian express") || lower.contains("bloomberg") || lower.contains("guardian") || lower.contains("nature")) {
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
        return query.replaceAll("(?i)\\b(shocking|secret|revealed|unbelievable|cure|fake|leaked|miracle|breaking|news|report)\\b", "")
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
