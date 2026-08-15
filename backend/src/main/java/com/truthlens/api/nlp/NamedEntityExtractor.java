package com.truthlens.api.nlp;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NamedEntityExtractor {

    private static final Set<String> KNOWN_ORGS = Set.of(
            "NASA", "WHO", "UN", "CDC", "FDA", "FBI", "CIA", "REUTERS", "ASSOCIATED PRESS",
            "PENTAGON", "WHITE HOUSE", "MIT", "HARVARD", "STANFORD", "OXFORD", "BBC", "CNN"
    );

    private static final Set<String> KNOWN_CONCEPTS = Set.of(
            "VACCINE", "CANCER", "COVID", "EXOPLANET", "DEEPFAKE", "QUANTUM", "ARTIFICIAL INTELLIGENCE",
            "CLIMATE CHANGE", "GLOBAL WARMING", "CRYPTOCURRENCY", "BITCOIN", "ELECTION", "5G"
    );

    public Map<String, String> extractEntities(String text) {
        Map<String, String> entityMap = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return entityMap;

        String upperText = text.toUpperCase();

        // 1. Check Known Organizations
        for (String org : KNOWN_ORGS) {
            if (upperText.contains(org)) {
                entityMap.put(org, "ORGANIZATION");
            }
        }

        // 2. Check Known Scientific & News Concepts
        for (String concept : KNOWN_CONCEPTS) {
            if (upperText.contains(concept)) {
                entityMap.put(concept, "CONCEPT");
            }
        }

        // 3. Extract Capitalized Named Entities (Persons & Locations heuristics)
        Pattern capitalizedWordsPattern = Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\b");
        Matcher matcher = capitalizedWordsPattern.matcher(text);

        int personCount = 0;
        while (matcher.find()) {
            String word = matcher.group(1);
            if (word.length() > 3 && !isCommonEnglishWord(word)) {
                if (!entityMap.containsKey(word.toUpperCase())) {
                    if (word.split("\\s+").length > 1 && personCount < 4) {
                        entityMap.put(word, "PERSON / LOCATION");
                        personCount++;
                    }
                }
            }
        }

        // 4. Extract Dates & Years
        Pattern datePattern = Pattern.compile("\\b(19|20)\\d{2}\\b|\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\b", Pattern.CASE_INSENSITIVE);
        Matcher dateMatcher = datePattern.matcher(text);
        while (dateMatcher.find()) {
            entityMap.put(dateMatcher.group(0), "DATE / TIME");
        }

        return entityMap;
    }

    private boolean isCommonEnglishWord(String word) {
        Set<String> stopWords = Set.of(
                "The", "This", "That", "There", "Here", "What", "When", "Where", "Which", "Who",
                "How", "Breaking", "Report", "News", "Today", "Yesterday", "According", "Official"
        );
        return stopWords.contains(word);
    }
}
