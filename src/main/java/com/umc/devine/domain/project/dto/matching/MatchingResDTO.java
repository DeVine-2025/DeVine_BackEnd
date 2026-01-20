package com.umc.devine.domain.project.dto.matching;

import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import lombok.Builder;

import java.time.LocalDateTime;

public class MatchingResDTO {


    @Builder
    public record ProposeResDTO (
        Long matchingId,
        Long projectId,
        String projectName,
        Long memberId,
        String memberNickname,
        MatchingStatus status,
        MatchingType matchingType,
        LocalDateTime createdAt
    ) {}
}
