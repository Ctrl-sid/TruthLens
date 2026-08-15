package com.truthlens.api.controller;

import com.truthlens.api.model.FactCheckHistory;
import com.truthlens.api.repository.FactCheckHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HistoryController {

    private final FactCheckHistoryRepository historyRepository;

    @GetMapping
    public ResponseEntity<List<FactCheckHistory>> getRecentHistory() {
        return ResponseEntity.ok(historyRepository.findTop10ByOrderByCreatedAtDesc());
    }
}
