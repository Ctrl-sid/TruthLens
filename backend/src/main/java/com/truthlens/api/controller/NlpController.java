package com.truthlens.api.controller;

import com.truthlens.api.dto.NlpAnalysisResponse;
import com.truthlens.api.nlp.NlpPipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NlpController {

    private final NlpPipelineService nlpPipelineService;

    @PostMapping("/analyze")
    public ResponseEntity<NlpAnalysisResponse> analyzeText(@RequestBody(required = false) String text) {
        String clean = text != null ? text.trim() : "";
        if (clean.startsWith("{") && clean.contains("\"text\"")) {
            int idx = clean.indexOf("\"text\"");
            int colon = clean.indexOf(":", idx);
            if (colon != -1) {
                int firstQuote = clean.indexOf("\"", colon);
                int lastQuote = clean.lastIndexOf("\"");
                if (firstQuote != -1 && lastQuote > firstQuote) {
                    clean = clean.substring(firstQuote + 1, lastQuote);
                }
            }
        } else if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() >= 2) {
            clean = clean.substring(1, clean.length() - 1);
        }
        return ResponseEntity.ok(nlpPipelineService.processText(clean));
    }
}
