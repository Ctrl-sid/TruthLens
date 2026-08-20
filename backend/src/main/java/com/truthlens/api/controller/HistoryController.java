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
import java.util.List;
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
                        .verdictBadgeColor(h.getGenuinenessScore() >= 75 ? "#10B981" : (h.getGenuinenessScore() >= 50 ? "#F59E0B" : "#EF4444"))
                        .rationale(h.getRationale())
                        .createdAt(h.getCreatedAt() != null ? h.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}

