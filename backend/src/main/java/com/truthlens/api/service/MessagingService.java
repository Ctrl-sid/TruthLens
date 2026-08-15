package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimFeedbackDTO;
import com.truthlens.api.dto.MessageDTO;
import com.truthlens.api.model.AdminMessage;
import com.truthlens.api.model.ClaimFeedback;
import com.truthlens.api.model.FactCheckHistory;
import com.truthlens.api.model.User;
import com.truthlens.api.repository.AdminMessageRepository;
import com.truthlens.api.repository.ClaimFeedbackRepository;
import com.truthlens.api.repository.FactCheckHistoryRepository;
import com.truthlens.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final UserRepository userRepository;
    private final AdminMessageRepository messageRepository;
    private final ClaimFeedbackRepository feedbackRepository;
    private final FactCheckHistoryRepository historyRepository;

    public MessageDTO.MessageResponse sendMessageToAdmin(MessageDTO.SendMessageRequest request, String username) {
        User sender = userRepository.findByUsername(username).orElseThrow();
        User admin = userRepository.findByUsername("admin")
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> "ROLE_ADMIN".equalsIgnoreCase(u.getRole()))
                        .findFirst()
                        .orElse(sender));

        FactCheckHistory claim = null;
        if (request.getClaimId() != null) {
            claim = historyRepository.findById(request.getClaimId()).orElse(null);
        }

        AdminMessage msg = AdminMessage.builder()
                .sender(sender)
                .receiver(admin)
                .claim(claim)
                .subject(request.getSubject() != null ? request.getSubject() : "Claim Dispute / Knowledge Share")
                .messageText(request.getMessageText())
                .claimContextSummary(request.getClaimContextSummary())
                .isRead(false)
                .build();

        messageRepository.save(msg);

        return MessageDTO.MessageResponse.builder()
                .id(msg.getId())
                .senderUsername(sender.getUsername())
                .senderFullName(sender.getFullName())
                .receiverUsername(admin.getUsername())
                .claimId(request.getClaimId())
                .subject(msg.getSubject())
                .messageText(msg.getMessageText())
                .claimContextSummary(msg.getClaimContextSummary())
                .isRead(false)
                .createdAt(msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public List<MessageDTO.MessageResponse> getUserMessages(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return messageRepository.findBySenderOrReceiverOrderByCreatedAtDesc(user, user).stream()
                .map(msg -> MessageDTO.MessageResponse.builder()
                        .id(msg.getId())
                        .senderUsername(msg.getSender().getUsername())
                        .senderFullName(msg.getSender().getFullName())
                        .receiverUsername(msg.getReceiver().getUsername())
                        .claimId(msg.getClaim() != null ? msg.getClaim().getId() : null)
                        .subject(msg.getSubject())
                        .messageText(msg.getMessageText())
                        .claimContextSummary(msg.getClaimContextSummary())
                        .isRead(msg.getIsRead())
                        .createdAt(msg.getCreatedAt() != null ? msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                        .build())
                .collect(Collectors.toList());
    }

    public ClaimFeedbackDTO.FeedbackResponse submitFeedback(ClaimFeedbackDTO.SubmitFeedbackRequest request, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        FactCheckHistory claim = null;
        if (request.getClaimId() != null) {
            claim = historyRepository.findById(request.getClaimId()).orElse(null);
        }

        ClaimFeedback fb = ClaimFeedback.builder()
                .user(user)
                .claim(claim)
                .rating(request.getRating())
                .flagReason(request.getFlagReason())
                .comments(request.getComments())
                .build();

        feedbackRepository.save(fb);

        // Also if comments or flag reason present, send message alert to Admin
        if (request.getComments() != null && !request.getComments().isBlank()) {
            User admin = userRepository.findByUsername("admin").orElse(user);
            AdminMessage msg = AdminMessage.builder()
                    .sender(user)
                    .receiver(admin)
                    .claim(claim)
                    .subject("Result Feedback & Rating (" + (request.getRating() != null ? request.getRating() + " Stars" : "Flagged") + ")")
                    .messageText(request.getComments())
                    .claimContextSummary("Flag Reason: " + (request.getFlagReason() != null ? request.getFlagReason() : "General Review"))
                    .isRead(false)
                    .build();
            messageRepository.save(msg);
        }

        return ClaimFeedbackDTO.FeedbackResponse.builder()
                .id(fb.getId())
                .username(user.getUsername())
                .claimId(request.getClaimId())
                .rating(fb.getRating())
                .flagReason(fb.getFlagReason())
                .comments(fb.getComments())
                .createdAt(fb.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }
}
