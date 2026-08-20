package com.truthlens.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MessageDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        private String recipientUsername; // optional if sending to admin
        private Long claimId;

        @NotBlank(message = "Subject is required")
        private String subject;

        @NotBlank(message = "Message text is required")
        private String messageText;

        private String claimContextSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private Long id;
        private String senderUsername;
        private String senderFullName;
        private String receiverUsername;
        private Long claimId;
        private String subject;
        private String messageText;
        private String claimContextSummary;
        private Boolean isRead;
        private String createdAt;
    }
}
