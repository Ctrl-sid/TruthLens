package com.truthlens.api.controller;

import com.truthlens.api.dto.ClaimFeedbackDTO;
import com.truthlens.api.dto.MessageDTO;
import com.truthlens.api.dto.UserModerationDTO;
import com.truthlens.api.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<UserModerationDTO.UserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<UserModerationDTO.UserSummaryResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UserModerationDTO.UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request, authentication.getName()));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/messages")
    public ResponseEntity<List<MessageDTO.MessageResponse>> getAdminInbox() {
        return ResponseEntity.ok(adminService.getAdminInbox());
    }

    @PostMapping("/messages/reply")
    public ResponseEntity<MessageDTO.MessageResponse> replyToUser(
            @RequestBody MessageDTO.SendMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminService.replyToUser(request, authentication.getName()));
    }

    @GetMapping("/feedback")
    public ResponseEntity<List<ClaimFeedbackDTO.FeedbackResponse>> getAllClaimFeedback() {
        return ResponseEntity.ok(adminService.getAllClaimFeedback());
    }
}
