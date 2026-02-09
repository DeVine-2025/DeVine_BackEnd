package com.umc.devine.domain.project.dto.matching;

import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import jakarta.validation.constraints.NotNull;

public class MatchingReqDTO {

    public record ProposeReqDTO (
            @NotNull(message = "프로젝트 ID는 필수입니다.")
            Long projectId
    ) {}

    public record DecisionReqDTO (
            @NotNull(message = "결정(ACCEPT/REJECT)은 필수입니다.")
            MatchingDecision decision
    ) {}
}
