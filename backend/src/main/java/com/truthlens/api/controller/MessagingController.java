package com.truthlens.api.controller;

import com.truthlens.api.dto.ClaimFeedbackDTO;
import com.truthlens.api.dto.MessageDTO;
import com.truthlens.api.service.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MessagingController {

    private final MessagingService messagingService;

    @PostMapping("/messages")
    public ResponseEntity<MessageDTO.MessageResponse> sendMessageToAdmin(
            @RequestBody MessageDTO.SendMessageRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "anonymousUser";
        return ResponseEntity.ok(messagingService.sendMessageToAdmin(request, username));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<MessageDTO.MessageResponse>> getUserMessages(Authentication authentication) {
        if (authentication == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(messagingService.getUserMessages(authentication.getName()));
    }

    @PostMapping("/feedback")
    public ResponseEntity<ClaimFeedbackDTO.FeedbackResponse> submitFeedback(
            @RequestBody ClaimFeedbackDTO.SubmitFeedbackRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "anonymousUser";
        return ResponseEntity.ok(messagingService.submitFeedback(request, username));
    }
}
