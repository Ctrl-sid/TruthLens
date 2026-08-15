package com.truthlens.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserModerationDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserStatusRequest {
        private String status; // ACTIVE, WARNED, BANNED
        private String reason; // Code of conduct violation explanation
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummaryResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private String status;
        private String createdAt;
    }
}
