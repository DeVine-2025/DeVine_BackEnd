package com.umc.devine.global.external.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GitHub Contribution (잔디) 정보 DTO
 * GitHub GraphQL API contributionCalendar 응답 매핑
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContributionDTO {

    /**
     * 기여 날짜 (YYYY-MM-DD 형식)
     */
    private String date;

    /**
     * 해당 날짜의 기여(커밋) 수
     */
    private Integer contributionCount;
}
