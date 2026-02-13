package com.umc.devine.domain.project.dto.matching;

import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MatchingReqDTO {

    public record ApplyReqDTO(
            @Schema(description = "지원 파트", example = "BACKEND")
            @NotNull(message = "지원 파트는 필수입니다.")
            ProjectPart part
    ) {}

    public record ProposeReqDTO(
            @Schema(description = "프로젝트 ID", example = "1")
            @NotNull(message = "프로젝트 ID는 필수입니다.")
            Long projectId,

            @Schema(description = "제안 파트", example = "BACKEND")
            @NotNull(message = "제안 파트는 필수입니다.")
            ProjectPart part,

            @Schema(description = "제안 내용", example = "저희 프로젝트에 백엔드 개발자로 함께하시면 좋겠습니다.")
            @Size(max = 500, message = "제안 내용은 500자를 초과할 수 없습니다.")
            String content
    ) {}

    public record DecisionReqDTO(
            @NotNull(message = "결정(ACCEPT/REJECT)은 필수입니다.")
            MatchingDecision decision
    ) {}
}
