package com.truthlens.api.dto;

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
    private String content; // raw text, url link, or base64 image string
    private String title; // optional headline
}
