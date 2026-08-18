package com.truthlens.api.nlp;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ClickbaitClassifier {

    private static final Set<String> CLICKBAIT_TRIGGERS = Set.of(
            "SHOCKING", "YOU WON'T BELIEVE", "SECRET REVEALED", "DOCTORS HATE THIS",
            "MIRACLE CURE", "WHAT HAPPENS NEXT", "DON'T WANT YOU TO KNOW", "MUST SEE",
            "GOES VIRAL", "MIND BLOWN", "INSANE TRICK", "HIDDEN TRUTH", "100% CURE",
            "THEY DON'T WANT YOU TO SEE", "SHARE BEFORE IT GETS DELETED", "BIG PHARMA IS HIDING",
            "GOVERNMENT DOESN'T WANT YOU TO KNOW", "SECRET PROTOCOL", "WHISTLEBLOWER EXPOSES",
            "LEAKED AUDIO", "SECRET TAPE", "SECRET PLAN", "SHOCKING PROOF", "PROOF THAT THE",
            "MAINSTREAM MEDIA WON'T TELL YOU", "COLLAPSE IMMINENT", "FREE MONEY FOR ALL",
            "GUARANTEED PROFIT", "MAGIC CURE", "ELIMINATE CANCER IN 24 HOURS", "CURES ALL DISEASES"
    );

    private static final Set<String> CONSPIRACY_PATTERNS = Set.of(
            "MICROCHIPS IN VACCINES", "FLAT EARTH", "5G CAUSES", "POISONING THE WATER",
            "NEW WORLD ORDER", "FAKE PANDEMIC", "STAGED EVENT", "POPULATION CONTROL",
            "SECRET PLOT", "CHEMTRAILS", "LIZARD PEOPLE", "RIGGED ELECTION", "FAKE MOON LANDING"
    );

    public double calculateClickbaitScore(String text, List<String> flagsOut) {
        if (text == null || text.isBlank()) return 0.0;
        String upperText = text.toUpperCase();

        int score = 0;

        // 1. Check Clickbait Triggers
        for (String trigger : CLICKBAIT_TRIGGERS) {
            if (upperText.contains(trigger)) {
                score += 25;
                if (flagsOut != null && !flagsOut.contains("Clickbait Trigger Phrase: '" + trigger + "'")) {
                    flagsOut.add("Clickbait Trigger Phrase: '" + trigger + "'");
                }
            }
        }

        // 2. Check Conspiracy Patterns
        for (String pattern : CONSPIRACY_PATTERNS) {
            if (upperText.contains(pattern)) {
                score += 35;
                if (flagsOut != null && !flagsOut.contains("Conspiracy Marker: '" + pattern + "'")) {
                    flagsOut.add("Conspiracy Marker: '" + pattern + "'");
                }
            }
        }

        // 3. Check Exclamation Marks and Question Marks
        long exclamationCount = text.chars().filter(ch -> ch == '!').count();
        if (exclamationCount >= 2) {
            score += 20;
            if (flagsOut != null) flagsOut.add("Excessive Exclamation Marks (" + exclamationCount + ")");
        }

        // 4. Check All-Caps Words Ratio
        String[] words = text.split("\\s+");
        int capsCount = 0;
        for (String word : words) {
            if (word.length() > 3 && word.equals(word.toUpperCase()) && Character.isLetter(word.charAt(0))) {
                capsCount++;
            }
        }

        if (words.length > 0 && (double) capsCount / words.length > 0.25) {
            score += 25;
            if (flagsOut != null) flagsOut.add("High Proportion of ALL-CAPS Words");
        }

        // 5. Sensationalist Superlatives
        if (upperText.contains("BEST EVER") || upperText.contains("WORST EVER") || upperText.contains("EVERYONE IS TALKING") || upperText.contains("UNBELIEVABLE")) {
            score += 15;
            if (flagsOut != null) flagsOut.add("Sensational Superlative Phrase");
        }

        return Math.min(100.0, score);
    }
}
