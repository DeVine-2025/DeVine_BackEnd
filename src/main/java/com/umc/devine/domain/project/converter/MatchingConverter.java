package com.umc.devine.domain.project.converter;

import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.enums.TechName;

import java.util.List;
import java.util.Map;

public class MatchingConverter {

    public static Matching toMatching(Project project, Member member, MatchingType matchingType, ProjectPart part, String content) {
        return Matching.builder()
                .project(project)
                .member(member)
                .status(MatchingStatus.PENDING)
                .matchingType(matchingType)
                .part(part)
                .content(content)
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
    public static MatchingResDTO.DeveloperMatchingInfo toDeveloperMatchingInfo(
            Matching matching,
            List<MemberCategory> memberCategories,
            List<DevTechstack> devTechstacks
    ) {
        Member developer = matching.getMember();
        Project project = matching.getProject();

        List<MatchingResDTO.CategoryInfo> categories = memberCategories.stream()
                .map(mc -> MatchingResDTO.CategoryInfo.builder()
                        .genre(mc.getCategory().getGenre())
                        .displayName(mc.getCategory().getGenre().getDisplayName())
                        .build())
                .toList();

        List<TechName> techStacks = devTechstacks.stream()
                .map(dt -> dt.getTechstack().getName())
                .toList();

        return MatchingResDTO.DeveloperMatchingInfo.builder()
                .matchingId(matching.getId())
                .projectId(project.getId())
                .projectName(project.getName())
                .developerId(developer.getId())
                .developerNickname(developer.getNickname())
                .developerImageUrl(developer.getImage())
                .part(matching.getPart())
                .partName(matching.getPart() != null ? matching.getPart().getDisplayName() : null)
                .categories(categories)
                .techStacks(techStacks)
                .body(developer.getBody())
                .matchingType(matching.getMatchingType())
                .decision(matching.getDecision())
                .createdAt(matching.getCreatedAt())
                .build();
    }

    // 개발자용: 프로젝트 매칭 정보 변환
    public static MatchingResDTO.ProjectMatchingInfo toProjectMatchingInfo(
            Matching matching,
            Map<Long, List<ProjectRequirementTechstack>> techstacksByRequirement
    ) {
        Project project = matching.getProject();

        List<MatchingResDTO.PositionInfo> positions = project.getRequirements().stream()
                .map(req -> {
                    List<TechName> techStacks = techstacksByRequirement
                            .getOrDefault(req.getId(), List.of()).stream()
                            .map(pt -> pt.getTechstack().getName())
                            .toList();

                    return MatchingResDTO.PositionInfo.builder()
                            .part(req.getPart())
                            .partName(req.getPart().getDisplayName())
                            .currentCount(req.getCurrentCount())
                            .requirementCount(req.getRequirementNum())
                            .techStacks(techStacks)
                            .build();
                })
                .toList();

        String thumbnailUrl = project.getImages().isEmpty()
                ? null
                : project.getImages().get(0).getImageUrl();

        return MatchingResDTO.ProjectMatchingInfo.builder()
                .matchingId(matching.getId())
                .projectId(project.getId())
                .projectName(project.getName())
                .thumbnailUrl(thumbnailUrl)
                .projectField(project.getProjectField())
                .projectFieldName(project.getProjectField().getDisplayName())
                .category(project.getCategory().getGenre())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .location(project.getLocation())
                .durationRange(project.getDurationRange())
                .durationRangeName(project.getDurationRange().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .positions(positions)
                .content(matching.getContent())
                .matchingType(matching.getMatchingType())
                .decision(matching.getDecision())
                .createdAt(matching.getCreatedAt())
                .build();
    }

    // 단순 매칭 상태 변환
    public static MatchingResDTO.MatchingStatusRes toMatchingStatusRes(Matching matching) {
        return MatchingResDTO.MatchingStatusRes.builder()
                .exists(true)
                .matchingId(matching.getId())
                .projectId(matching.getProject().getId())
                .status(matching.getStatus())
                .build();
    }
}
