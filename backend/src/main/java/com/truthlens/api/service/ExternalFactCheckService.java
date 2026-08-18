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
            // Clean up query terms for search
            String simplifiedQuery = cleanSearchQuery(query);
            if (simplifiedQuery.length() < 3) return Optional.empty();

            String encodedQuery = URLEncoder.encode(simplifiedQuery, StandardCharsets.UTF_8);
            String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "TruthLens-FactCheckEngine/1.0 (contact@truthlens.ai)")
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                String extract = extractJsonField(body, "extract");
                String title = extractJsonField(body, "title");
                String pageUrl = extractJsonField(body, "content_urls", "page");

                if (extract != null && !extract.isBlank() && !extract.toLowerCase().contains("may refer to")) {
                    return Optional.of(ExternalFactResult.builder()
                            .topic(title != null ? title : simplifiedQuery)
                            .snippet(extract)
                            .sourceName("Wikipedia Knowledge Archive")
                            .sourceUrl(pageUrl != null ? pageUrl : "https://en.wikipedia.org/wiki/" + encodedQuery)
                            .isAuthenticCorroboration(true)
                            .build());
                }
            }
        } catch (Exception e) {
            // Graceful fallback to offline NLP corpus
            log.debug("External knowledge lookup skipped or timed out: {}", e.getMessage());
        }

        return Optional.empty();
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
