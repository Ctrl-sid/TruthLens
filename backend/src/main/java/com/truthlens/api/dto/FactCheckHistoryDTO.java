package com.truthlens.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactCheckHistoryDTO {
    private Long id;
    private String username;
    private String inputType;
    private String inputContent;
    private String claimSummary;
    private Integer genuinenessScore;
    private String verdict;
    private String verdictBadgeColor;
    private String rationale;
    private String createdAt;
}
