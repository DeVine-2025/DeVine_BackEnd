package com.umc.devine.global.external.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GitHub 레포지토리 정보 응답 DTO
 * GitHub REST API v3 /user/repos 엔드포인트 응답 매핑
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositoryDTO {

    /**
     * 레포지토리 이름
     */
    private String name;

    /**
     * 레포지토리 설명
     */
    private String description;

    /**
     * 레포지토리 URL (https://github.com/...)
     */
    @JsonProperty("html_url")
    private String htmlUrl;
}
