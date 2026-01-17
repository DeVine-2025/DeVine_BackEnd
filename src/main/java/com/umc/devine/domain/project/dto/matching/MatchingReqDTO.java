package com.umc.devine.domain.project.dto.matching;

import jakarta.validation.constraints.NotNull;

public class MatchingReqDTO {

    public record ProposeReqDTO (
            @NotNull(message = "프로젝트 ID는 필수입니다.")
            Long projectId
    ) {}
}
