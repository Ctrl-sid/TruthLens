package com.truthlens.api.controller;

import com.truthlens.api.dto.FactCheckHistoryDTO;
import com.truthlens.api.model.FactCheckHistory;
import com.truthlens.api.model.User;
import com.truthlens.api.repository.FactCheckHistoryRepository;
import com.truthlens.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HistoryController {

    private final FactCheckHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<FactCheckHistoryDTO>> getRecentHistory(Authentication authentication) {
        List<FactCheckHistory> historyList;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                historyList = historyRepository.findByUserOrderByCreatedAtDesc(user);
            } else {
                historyList = historyRepository.findTop10ByOrderByCreatedAtDesc();
            }
        } else {
            historyList = historyRepository.findTop10ByOrderByCreatedAtDesc();
        }

        List<FactCheckHistoryDTO> dtos = historyList.stream()
                .map(h -> FactCheckHistoryDTO.builder()
                        .id(h.getId())
                        .username(h.getUser() != null ? h.getUser().getUsername() : "Anonymous")
                        .inputType(h.getInputType())
                        .inputContent(h.getInputContent())
                        .claimSummary(h.getClaimSummary())
                        .genuinenessScore(h.getGenuinenessScore())
                        .verdict(h.getVerdict())
                        .verdictBadgeColor(h.getGenuinenessScore() != null && h.getGenuinenessScore() >= 75 ? "#10B981" : (h.getGenuinenessScore() != null && h.getGenuinenessScore() >= 50 ? "#F59E0B" : "#EF4444"))
                        .rationale(h.getRationale())
                        .createdAt(h.getCreatedAt() != null ? h.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * DPDP Act 2023 - Data Portability / User Data Export
     * Exports full fact-check history and query logs for authenticated user in machine-readable JSON.
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportUserData(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required for data export"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<FactCheckHistory> userHistory = historyRepository.findByUserOrderByCreatedAtDesc(user);
        List<Map<String, Object>> exportedRecords = userHistory.stream().map(h -> {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("recordId", h.getId());
            record.put("inputType", h.getInputType());
            record.put("inputContent", h.getInputContent());
            record.put("claimSummary", h.getClaimSummary());
            record.put("verdict", h.getVerdict());
            record.put("genuinenessScore", h.getGenuinenessScore());
            record.put("rationale", h.getRationale());
            record.put("verifiedAt", h.getCreatedAt() != null ? h.getCreatedAt().toString() : "");
            return record;
        }).collect(Collectors.toList());

        Map<String, Object> exportPayload = new LinkedHashMap<>();
        exportPayload.put("dpdpComplianceVersion", "DPDP-2023-V1");
        exportPayload.put("userId", user.getId());
        exportPayload.put("username", user.getUsername());
        exportPayload.put("email", user.getEmail());
        exportPayload.put("exportTimestamp", java.time.LocalDateTime.now().toString());
        exportPayload.put("totalRecords", userHistory.size());
        exportPayload.put("factCheckRecords", exportedRecords);

        return ResponseEntity.ok(exportPayload);
    }

    /**
     * DPDP Act 2023 - Right to Erasure / Purge All History
     */
    @DeleteMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> purgeUserHistory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required for record purge"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        historyRepository.deleteByUser(user);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "All user verification records purged successfully under DPDP Act"));
    }

    /**
     * Delete individual record
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        FactCheckHistory history = historyRepository.findById(id).orElse(null);
        if (history == null) {
            return ResponseEntity.notFound().build();
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null && history.getUser() != null && !history.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to delete this record"));
        }

        historyRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "deletedId", id));
    }
}

