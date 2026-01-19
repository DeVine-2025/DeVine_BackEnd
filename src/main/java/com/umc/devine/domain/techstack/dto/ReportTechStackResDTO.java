package com.umc.devine.domain.techstack.dto;

import lombok.Builder;

public class ReportTechStackResDTO {
    // TODO: 합쳐지면 양식 통합하기
    @Builder
    public record ReportTechstackDTO(
            String techstackName,
            String techGenre,
            Integer rate
    ){}
}
