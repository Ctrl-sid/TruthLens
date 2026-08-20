package com.truthlens.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimVerificationRequest {
    private String type; // TEXT, URL, IMAGE

    @NotBlank(message = "Content payload cannot be empty")
    private String content; // raw text, url link, or base64 image string

    private String title; // optional headline
}
