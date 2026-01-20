package com.umc.devine.domain.project.converter;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;

public class MatchingConverter {

    public static Matching toMatching(Project project, Member member, MatchingType matchingType) {
        return Matching.builder()
                .project(project)
                .member(member)
                .status(MatchingStatus.PENDING)
                .matchingType(matchingType)
                .build();
    }

    public static MatchingResDTO.ProposeResDTO toMatchingResDTO(Matching matching) {
        return MatchingResDTO.ProposeResDTO.builder()
                .matchingId(matching.getId())
                .projectId(matching.getProject().getId())
                .projectName(matching.getProject().getName())
                .memberId(matching.getMember().getId())
                .memberNickname(matching.getMember().getNickname())
                .status(matching.getStatus())
                .matchingType(matching.getMatchingType())
                .createdAt(matching.getCreatedAt())
                .build();
    }
}
