package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import org.springframework.data.domain.Pageable;

public interface MatchingQueryService {

    // PM용: 개발자 목록 조회 (제안한/지원받은)
    MatchingResDTO.DevelopersRes getDevelopers(Member pm, MatchingType type, Pageable pageable);

    // 개발자용: 프로젝트 목록 조회 (제안받은/지원한)
    MatchingResDTO.ProjectsRes getProjects(Member developer, MatchingType type, Pageable pageable);

    // 개발자용: 본인의 지원 상태 조회 (APPLY 타입만)
    MatchingResDTO.MatchingStatusRes getMyApplyStatus(Member member, Long projectId);

    // PM용: 특정 개발자에게 제안한 상태 조회 (PROPOSE 타입만)
    MatchingResDTO.MatchingStatusRes getMyProposeStatus(Member pm, Long projectId, Long memberId);
}
