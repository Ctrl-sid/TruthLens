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
    public ResponseEntity<NlpAnalysisResponse> analyzeText(@RequestBody String text) {
        return ResponseEntity.ok(nlpPipelineService.processText(text));
    }
}
