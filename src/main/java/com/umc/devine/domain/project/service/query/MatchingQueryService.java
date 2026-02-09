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
}
