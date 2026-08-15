package com.truthlens.api.controller;

import com.truthlens.api.dto.ClaimVerificationRequest;
import com.truthlens.api.dto.ClaimVerificationResponse;
import com.truthlens.api.service.FactCheckEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FactCheckController {

    private final FactCheckEngineService factCheckEngineService;

    @PostMapping("/claim")
    public ResponseEntity<ClaimVerificationResponse> verifyClaim(@RequestBody ClaimVerificationRequest request) {
        return ResponseEntity.ok(factCheckEngineService.verifyClaim(request));
    }
}
