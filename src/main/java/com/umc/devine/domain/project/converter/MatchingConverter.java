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

    // PM용: 개발자 매칭 정보 변환
    public static MatchingResDTO.DeveloperMatchingInfo toDeveloperMatchingInfo(Matching matching) {
        Member developer = matching.getMember();
        Project project = matching.getProject();

        return MatchingResDTO.DeveloperMatchingInfo.builder()
                .matchingId(matching.getId())
                .projectId(project.getId())
                .projectName(project.getName())
                .developerId(developer.getId())
                .developerNickname(developer.getNickname())
                .developerImageUrl(developer.getImage())
                .status(matching.getStatus())
                .matchingType(matching.getMatchingType())
                .decision(matching.getDecision())
                .createdAt(matching.getCreatedAt())
                .build();
    }

    // 개발자용: 프로젝트 매칭 정보 변환
    public static MatchingResDTO.ProjectMatchingInfo toProjectMatchingInfo(Matching matching) {
        Project project = matching.getProject();
        Member pm = project.getMember();

        return MatchingResDTO.ProjectMatchingInfo.builder()
                .matchingId(matching.getId())
                .projectId(project.getId())
                .projectName(project.getName())
                .pmId(pm.getId())
                .pmNickname(pm.getNickname())
                .pmImageUrl(pm.getImage())
                .status(matching.getStatus())
                .matchingType(matching.getMatchingType())
                .decision(matching.getDecision())
                .createdAt(matching.getCreatedAt())
                .build();
    }
}
