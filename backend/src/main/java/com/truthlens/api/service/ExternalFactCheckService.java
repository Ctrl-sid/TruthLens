package com.truthlens.api.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ExternalFactCheckService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Getter
    @Builder
    public static class ExternalFactResult {
        private String topic;
        private String snippet;
        private String sourceUrl;
        private String sourceName;
        private boolean isAuthenticCorroboration;
    }

    public Optional<ExternalFactResult> queryExternalKnowledge(String query) {
        if (query == null || query.isBlank()) return Optional.empty();

        try {
            String simplifiedQuery = cleanSearchQuery(query);
            if (simplifiedQuery.length() < 3) return Optional.empty();

            String encodedQuery = URLEncoder.encode(simplifiedQuery, StandardCharsets.UTF_8);
            // Use Wikipedia OpenSearch API for flexible natural language discovery
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
                // Format: ["query", ["Title"], ["Snippet / Extract"], ["URL"]]
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
                            .sourceUrl(pageUrl)
                            .isAuthenticCorroboration(true)
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("External knowledge lookup skipped: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private List<String> extractJsonArrayItems(String json, int arrayIndex) {
        List<String> results = new ArrayList<>();
        try {
            // Primitive parser for standard OpenSearch format: [query, [titles], [snippets], [urls]]
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

    private String cleanSearchQuery(String query) {
        // Take core 3-5 nouns/keywords
        String cleaned = query.replaceAll("(?i)\\b(shocking|secret|revealed|unbelievable|cure|fake|leaked|miracle|breaking|news|report)\\b", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        String[] words = cleaned.split(" ");
        if (words.length > 4) {
            return String.join(" ", words[0], words[1], words[2], words[3]);
        }
        return cleaned;
    }

    private String extractJsonField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"");
        }
        return null;
    }

    private String extractJsonField(String json, String parentField, String childField) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(childField) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"");
        }
        return null;
    }
}
