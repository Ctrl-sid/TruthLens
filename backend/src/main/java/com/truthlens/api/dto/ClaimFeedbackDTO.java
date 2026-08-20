package com.truthlens.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ClaimFeedbackDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitFeedbackRequest {
        private Long claimId;

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        private Integer rating; // 1 to 5 stars

        private String flagReason; // INACCURATE_FACT, CULTURALLY_INAPPROPRIATE, PERSONAL_BIAS, OTHER
        private String comments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackResponse {
        private Long id;
        private String username;
        private Long claimId;
        private Integer rating;
        private String flagReason;
        private String comments;
        private String createdAt;
    }
}
