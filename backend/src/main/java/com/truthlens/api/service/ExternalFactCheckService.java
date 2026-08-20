package com.truthlens.api.service;

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
    }

    public Optional<ExternalFactResult> queryExternalKnowledge(String query) {
        if (query == null || query.isBlank()) return Optional.empty();

        // 1. First Priority: Live News Wire & Major Press Search (Google News RSS Feed)
        Optional<ExternalFactResult> liveNewsMatch = queryLiveNewsWires(query);
        if (liveNewsMatch.isPresent()) {
            return liveNewsMatch;
        }

        // 2. Second Priority: Wikipedia OpenSearch & Encyclopedic Knowledge
        return queryWikipedia(query);
    }

    public Optional<ExternalFactResult> queryLiveNewsWires(String query) {
        try {
            String cleanQuery = cleanSearchQuery(query);
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
                return parseGoogleNewsRss(response.body(), query);
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

                double overlap = calculateQueryArticleOverlap(originalQuery, title);
                if (overlap > bestOverlap) {
                    bestOverlap = overlap;
                    bestTitle = title;
                    bestLink = link;
                    bestSource = currentSource;
                    bestSourceUrl = sourceUrl;
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

            ContradictionCheck contradiction = detectContradiction(originalQuery, bestTitle);
            if (contradiction.isContradicted()) {
                return Optional.of(ExternalFactResult.builder()
                        .topic(bestTitle)
                        .snippet("Contradicted by verified press reporting from " + bestSource + ": \"" + bestTitle + "\". " + contradiction.getReason())
                        .sourceName(bestSource)
                        .sourceDomain(resolvedDomain)
                        .sourceUrl(bestLink != null ? bestLink : (bestSourceUrl != null ? bestSourceUrl : "https://news.google.com"))
                        .isAuthenticCorroboration(false)
                        .isContradiction(true)
                        .contradictionDetail(contradiction.getReason())
                        .credibilityScore(cred)
                        .matchPercentage(matchPct)
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
            if (qNum == 0 && aNum > 0) {
                return new ContradictionCheck(true, "Claim asserts zero / none, whereas verified coverage confirms " + aNum + ".");
            } else if (Math.abs(qNum - aNum) >= 3 && Math.max(qNum, aNum) >= 5) {
                return new ContradictionCheck(true, "Significant discrepancy in reported figures (claim states " + qNum + ", news reports " + aNum + ").");
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
        for (String token : text.toLowerCase().split("[^a-zA-Z0-9]+")) {
            if (wordToNum.containsKey(token)) {
                return wordToNum.get(token);
            }
            try {
                return Integer.parseInt(token);
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
        try {
            String simplifiedQuery = cleanSearchQuery(query);
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

                    return Optional.of(ExternalFactResult.builder()
                            .topic(title)
                            .snippet(snippet)
                            .sourceName("Wikipedia Knowledge Archive")
                            .sourceDomain("wikipedia.org")
                            .sourceUrl(pageUrl)
                            .isAuthenticCorroboration(true)
                            .credibilityScore(94)
                            .matchPercentage(90.0)
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("Wikipedia knowledge lookup skipped: {}", e.getMessage());
        }
        return Optional.empty();
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
