package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimVerificationResponse.ClaimOriginDiscovery;
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
    }

    public Optional<ExternalFactResult> queryExternalKnowledge(String query) {
        if (query == null || query.isBlank()) return Optional.empty();

        // 1. Direct Live News Wire Search
        Optional<ExternalFactResult> liveNewsMatch = queryLiveNewsWires(query, query);
        if (liveNewsMatch.isPresent()) {
            return liveNewsMatch;
        }

        // 2. Direct Wikipedia OpenSearch
        Optional<ExternalFactResult> wikiMatch = queryWikipedia(query, query);
        if (wikiMatch.isPresent()) {
            return wikiMatch;
        }

        // 3. Fallback: If claim contains negations or zero-quantifiers (e.g. "Mumbai terror attack, None killed"),
        // search for the underlying origin story (e.g. "Mumbai terror attack") and cross-reference against the prompt!
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

            if (title != null && !title.isBlank()) {
                title = unescapeXml(title);
                // Extract source name from title if source tag is missing or generic (e.g. "Headline - The Hindu")
                if (title.contains(" - ")) {
                    int lastDash = title.lastIndexOf(" - ");
                    if (source == null || source.isBlank()) {
                        source = title.substring(lastDash + 3).trim();
                    }
                    title = title.substring(0, lastDash).trim();
                }

                String currentSource = (source != null && !source.isBlank()) ? unescapeXml(source) : "Accredited News Wire";
                String resolvedDomain = extractDomain(sourceUrl);
                if (resolvedDomain == null || resolvedDomain.isBlank()) {
                    resolvedDomain = extractDomainFromSourceName(currentSource);
                }

                // Check if this publisher is Tier-1 accredited - skip unverified blogs/substacks to find true Tier-1 hits!
                if (!isTier1AccreditedPublisher(currentSource, resolvedDomain)) {
                    continue;
                }

                // Verify predicate action alignment: if query alleges death/arrest, headline must also report that event
                String qLower = originalQuery.toLowerCase();
                String tLower = title.toLowerCase();

                boolean queryClaimsDeath = qLower.contains("passed away") || qLower.contains("died") || qLower.contains("dead") || qLower.contains("killed") || qLower.contains("death") || qLower.contains("assassinated");
                if (queryClaimsDeath) {
                    boolean subjectIsMourningSomeoneElse = tLower.contains("saddened by") || tLower.contains("mourns") || 
                                                           tLower.contains("condoles") || tLower.contains("pays tribute") || 
                                                           tLower.contains("condolence") || tLower.contains("grieves") || 
                                                           tLower.contains("reacts to death");
                    if (subjectIsMourningSomeoneElse) {
                        continue; // Subject is alive and mourning someone else!
                    }

                    boolean titleMentionsDeath = tLower.contains("passed away") || tLower.contains("dies") || tLower.contains("dead") || tLower.contains("killed") || tLower.contains("death of") || tLower.contains("obituary") || tLower.contains("assassinated");
                    if (!titleMentionsDeath) {
                        continue; // Headline does not report the alleged death!
                    }
                }

                boolean queryClaimsArrest = qLower.contains("arrested") || qLower.contains("detained") || qLower.contains("jailed") || qLower.contains("indicted");
                if (queryClaimsArrest) {
                    boolean titleMentionsArrest = tLower.contains("arrested") || tLower.contains("detained") || tLower.contains("jailed") || tLower.contains("indicted") || tLower.contains("charges");
                    if (!titleMentionsArrest) {
                        continue;
                    }
                }

                double overlap = calculateQueryArticleOverlap(originalQuery, title);
                ContradictionCheck itemContradiction = detectContradiction(originalQuery, title);

                if (overlap >= 0.38) {
                    String domainKey = resolvedDomain != null ? resolvedDomain : currentSource;
                    if (!seenDomains.contains(domainKey)) {
                        seenDomains.add(domainKey);
                        crossReferencedList.add(SourceEvidence.builder()
                                .sourceName(currentSource)
                                .domain(resolvedDomain != null ? resolvedDomain : "news.google.com")
                                .articleTitle(title)
                                .url(link != null ? link : (sourceUrl != null ? sourceUrl : "https://news.google.com"))
                                .credibilityRating(determineSourceCredibility(currentSource))
                                .matchPercentage(Math.min(99.0, Math.round((72.0 + (overlap * 25.0)) * 10.0) / 10.0))
                                .verdictBySource(itemContradiction.isContradicted() ? "Contradicted / False" : "Verified True")
                                .build());
                    }
                }

                if (overlap > bestOverlap) {
                    bestOverlap = overlap;
                    bestTitle = title;
                    bestLink = link;
                    bestSource = currentSource;
                    bestSourceUrl = sourceUrl;
                    bestContradiction = itemContradiction;
                }
            }
        }

        // Require at least 45% meaningful keyword overlap from verified Tier-1 publisher
        if (bestOverlap >= 0.45 && bestTitle != null) {
            String resolvedDomain = extractDomain(bestSourceUrl);
            if (resolvedDomain == null || resolvedDomain.isBlank()) {
                resolvedDomain = extractDomainFromSourceName(bestSource);
            }

            int cred = determineSourceCredibility(bestSource);
            double matchPct = Math.min(99.0, Math.round((74.0 + (bestOverlap * 24.0)) * 10.0) / 10.0);

            boolean isContradicted = bestContradiction != null && bestContradiction.isContradicted();

            ClaimOriginDiscovery originDiscovery = ClaimOriginDiscovery.builder()
                    .originalPublisher(bestSource)
                    .originalDomain(resolvedDomain)
                    .originalHeadline(bestTitle)
                    .originalUrl(bestLink != null ? bestLink : (bestSourceUrl != null ? bestSourceUrl : "https://news.google.com"))
                    .publishedDate("Verified Press Dispatch")
                    .provenanceType(isContradicted ? "ALTERED_DISTORTION" : "AUTHENTIC_REPRODUCTION")
                    .distortionAnalysis(isContradicted ?
                            "Claim derived from a verified news event reported by " + bestSource + ", but altered: " + bestContradiction.getReason() :
                            "Claim faithfully matches verified reporting originally published by " + bestSource + ".")
                    .crossReferencedConsensus(isContradicted ?
                            "Contradicted across accredited news wire reports." :
                            "Cross-referenced and corroborated across " + Math.max(1, crossReferencedList.size()) + " accredited news wire agencies.")
                    .originMatchConfidence(matchPct)
                    .build();

            if (isContradicted) {
                return Optional.of(ExternalFactResult.builder()
                        .topic(bestTitle)
                        .snippet("Contradicted by verified press reporting from " + bestSource + ": \"" + bestTitle + "\". " + bestContradiction.getReason())
                        .sourceName(bestSource)
                        .sourceDomain(resolvedDomain)
                        .sourceUrl(bestLink != null ? bestLink : (bestSourceUrl != null ? bestSourceUrl : "https://news.google.com"))
                        .isAuthenticCorroboration(false)
                        .isContradiction(true)
                        .contradictionDetail(bestContradiction.getReason())
                        .credibilityScore(cred)
                        .matchPercentage(matchPct)
                        .originDiscovery(originDiscovery)
                        .crossReferencedSources(crossReferencedList)
                        .build());
            }

            return Optional.of(ExternalFactResult.builder()
                    .topic(bestTitle)
                    .snippet("Corroborated by verified press reporting from " + bestSource + ": \"" + bestTitle + "\".")
                    .sourceName(bestSource)
                    .sourceDomain(resolvedDomain)
                    .sourceUrl(bestLink != null ? bestLink : (bestSourceUrl != null ? bestSourceUrl : "https://news.google.com"))
                    .isAuthenticCorroboration(true)
                    .isContradiction(false)
                    .credibilityScore(cred)
                    .matchPercentage(matchPct)
                    .originDiscovery(originDiscovery)
                    .crossReferencedSources(crossReferencedList)
                    .build());
        }

        return Optional.empty();
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
        // Query asserts: "none", "no one", "nobody", "zero", "0", "no casualties", "killed none", "not killed", "no deaths"
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
            // Check if the article actually contains qNum anywhere in text
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

    public boolean isTier1AccreditedPublisher(String sourceName, String domain) {
        if (sourceName == null && domain == null) return false;
        String s = (sourceName != null ? sourceName.toLowerCase() : "");
        String d = (domain != null ? domain.toLowerCase() : "");

        // Explicitly reject blogs, substack, personal domains, and forums
        if (d.contains("substack.com") || d.contains("medium.com") || d.contains("blogspot.com") ||
            d.contains("wordpress.com") || d.contains("tumblr.com") || d.contains("tomaspueyo.com") ||
            d.contains("reddit.com") || d.contains("quora.com") || d.contains("twitter.com") || d.contains("x.com")) {
            return false;
        }

        // Recognized Tier-1 accredited news wires, broadcasters, and reputable press networks
        return s.contains("hindu") || s.contains("reuters") || s.contains("associated press") ||
               s.contains("ap news") || s.contains("pti") || s.contains("ani") || s.contains("bbc") ||
               s.contains("indian express") || s.contains("bloomberg") || s.contains("guardian") ||
               s.contains("nature") || s.contains("science") || s.contains("ndtv") ||
               s.contains("times of india") || s.contains("hindustan times") || s.contains("cnn") ||
               s.contains("times now") || s.contains("india today") || s.contains("republic") ||
               s.contains("zee") || s.contains("news18") || s.contains("business standard") ||
               s.contains("economic times") || s.contains("abp") || s.contains("dna india") ||
               s.contains("firstpost") || s.contains("mathrubhumi") || s.contains("asianet") ||
               s.contains("free press journal") || s.contains("financial express") || s.contains("livemint") ||
               s.contains("deccan herald") || s.contains("al jazeera") || s.contains("wall street journal") ||
               s.contains("washington post") || s.contains("new york times") || s.contains("abc news") ||
               s.contains("cbs news") || s.contains("nbc news") || s.contains("afp") ||
               s.contains("snopes") || s.contains("politifact") ||
               d.contains("thehindu.com") || d.contains("reuters.com") || d.contains("apnews.com") ||
               d.contains("bbc.com") || d.contains("indianexpress.com") || d.contains("timesofindia.indiatimes.com") ||
               d.contains("economictimes.indiatimes.com") || d.contains("ndtv.com") || d.contains("hindustantimes.com") ||
               d.contains("indiatoday.in") || d.contains("bloomberg.com") || d.contains("theguardian.com") ||
               d.contains("nature.com") || d.contains("republicworld.com") || d.contains("timesnownews.com") ||
               d.contains("abplive.com") || d.contains("dnaindia.com") || d.contains("firstpost.com") ||
               d.contains("snopes.com") || d.contains("politifact.com");
    }

    private Optional<ExternalFactResult> queryWikipedia(String query) {
        return queryWikipedia(query, query);
    }

    private Optional<ExternalFactResult> queryWikipedia(String searchQuery, String originalQuery) {
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
                                    .articleTitle(title)
                                    .url(pageUrl)
                                    .credibilityRating(94)
                                    .matchPercentage(90.0)
                                    .verdictBySource(isContradicted ? "Contradicted / False" : (corroborates ? "Verified True" : "Unverified"))
                                    .build()))
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

        // 1. Death / Passing assertion
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

        // 2. Cure / Miracle claim
        boolean claimsCure = qLower.contains("cure") || qLower.contains("cures") || qLower.contains("miracle");
        if (claimsCure && !sLower.contains("cure") && !sLower.contains("therapy") && !sLower.contains("approved")) {
            return false;
        }

        // 3. Significant token overlap
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
