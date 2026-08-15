package com.truthlens.api.service;

import com.truthlens.api.dto.ClaimFeedbackDTO;
import com.truthlens.api.dto.MessageDTO;
import com.truthlens.api.dto.UserModerationDTO;
import com.truthlens.api.model.AdminMessage;
import com.truthlens.api.model.ClaimFeedback;
import com.truthlens.api.model.User;
import com.truthlens.api.repository.AdminMessageRepository;
import com.truthlens.api.repository.ClaimFeedbackRepository;
import com.truthlens.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AdminMessageRepository messageRepository;
    private final ClaimFeedbackRepository feedbackRepository;

    public List<UserModerationDTO.UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserModerationDTO.UserSummaryResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                        .build())
                .collect(Collectors.toList());
    }

    public UserModerationDTO.UserSummaryResponse updateUserStatus(Long userId, UserModerationDTO.UpdateUserStatusRequest request, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Cannot modify status of another Admin superuser!");
        }

        user.setStatus(request.getStatus().toUpperCase());
        userRepository.save(user);

        // Optionally send automated system notification message to the user
        User admin = userRepository.findByUsername(adminUsername).orElseThrow();
        String noticeSubject = "Account Status Notice: " + user.getStatus();
        String noticeBody = "Your account status has been updated to " + user.getStatus() + ". Reason: " + 
                (request.getReason() != null ? request.getReason() : "Code of Conduct compliance check.");

        AdminMessage notice = AdminMessage.builder()
                .sender(admin)
                .receiver(user)
                .subject(noticeSubject)
                .messageText(noticeBody)
                .claimContextSummary("Official Platform Moderation Action")
                .isRead(false)
                .build();
        messageRepository.save(notice);

        return UserModerationDTO.UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                .build();
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Cannot delete Admin superuser!");
        }

        userRepository.delete(user);
    }

    public List<MessageDTO.MessageResponse> getAdminInbox() {
        return messageRepository.findTop50ByOrderByCreatedAtDesc().stream()
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

    public MessageDTO.MessageResponse replyToUser(MessageDTO.SendMessageRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername).orElseThrow();
        User recipient = userRepository.findByUsername(request.getRecipientUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getRecipientUsername()));

        AdminMessage reply = AdminMessage.builder()
                .sender(admin)
                .receiver(recipient)
                .subject(request.getSubject() != null ? request.getSubject() : "RE: Admin Response")
                .messageText(request.getMessageText())
                .claimContextSummary(request.getClaimContextSummary())
                .isRead(false)
                .build();

        messageRepository.save(reply);

        return MessageDTO.MessageResponse.builder()
                .id(reply.getId())
                .senderUsername(admin.getUsername())
                .senderFullName(admin.getFullName())
                .receiverUsername(recipient.getUsername())
                .claimId(request.getClaimId())
                .subject(reply.getSubject())
                .messageText(reply.getMessageText())
                .claimContextSummary(reply.getClaimContextSummary())
                .isRead(false)
                .createdAt(reply.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public List<ClaimFeedbackDTO.FeedbackResponse> getAllClaimFeedback() {
        return feedbackRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(fb -> ClaimFeedbackDTO.FeedbackResponse.builder()
                        .id(fb.getId())
                        .username(fb.getUser().getUsername())
                        .claimId(fb.getClaim() != null ? fb.getClaim().getId() : null)
                        .rating(fb.getRating())
                        .flagReason(fb.getFlagReason())
                        .comments(fb.getComments())
                        .createdAt(fb.getCreatedAt() != null ? fb.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                        .build())
                .collect(Collectors.toList());
    }
}
