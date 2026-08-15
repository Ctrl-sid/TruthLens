package com.truthlens.api.controller;

import com.truthlens.api.model.VerifiedSource;
import com.truthlens.api.repository.VerifiedSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SourceController {

    private final VerifiedSourceRepository sourceRepository;

    @GetMapping
    public ResponseEntity<List<VerifiedSource>> getAllSources() {
        return ResponseEntity.ok(sourceRepository.findAll());
    }
}
