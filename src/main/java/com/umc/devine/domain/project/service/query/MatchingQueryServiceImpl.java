package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.converter.MatchingConverter;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorCode;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingQueryServiceImpl implements MatchingQueryService {

    private final MatchingRepository matchingRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;

    @Override
    public MatchingResDTO.DevelopersRes getDevelopers(Member pm, MatchingType type, Pageable pageable) {
        Page<Matching> matchingPage = matchingRepository.findByProjectOwnerAndMatchingType(
                pm, type, MatchingStatus.CANCELLED, pageable
        );

        List<MatchingResDTO.DeveloperMatchingInfo> developers = matchingPage.getContent().stream()
                .map(MatchingConverter::toDeveloperMatchingInfo)
                .toList();

        return MatchingResDTO.DevelopersRes.builder()
                .developers(PagedResponse.of(matchingPage, developers))
                .build();
    }

    @Override
    public MatchingResDTO.ProjectsRes getProjects(Member developer, MatchingType type, Pageable pageable) {
        Page<Matching> matchingPage = matchingRepository.findByMemberAndMatchingType(
                developer, type, MatchingStatus.CANCELLED, pageable
        );

        List<MatchingResDTO.ProjectMatchingInfo> projects = matchingPage.getContent().stream()
                .map(MatchingConverter::toProjectMatchingInfo)
                .toList();

        return MatchingResDTO.ProjectsRes.builder()
                .projects(PagedResponse.of(matchingPage, projects))
                .build();
    }

    @Override
    public MatchingResDTO.MatchingStatusRes getMyApplyStatus(Member member, Long projectId) {
        // 1. 프로젝트 존재 확인
        if (!projectRepository.existsById(projectId)) {
            throw new MatchingException(MatchingErrorCode.PROJECT_NOT_FOUND);
        }

        // 2. APPLY 타입, CANCELLED 제외하고 본인의 매칭 조회
        Optional<Matching> matchingOpt = matchingRepository
                .findByProjectIdAndMemberIdAndTypeAndStatusNot(
                        projectId, member.getId(), MatchingType.APPLY, MatchingStatus.CANCELLED);

        // 3. 매칭 없으면 exists=false 응답 반환
        return matchingOpt
                .map(MatchingConverter::toMatchingStatusRes)
                .orElse(MatchingResDTO.MatchingStatusRes.notFound(projectId));
    }

    @Override
    public MatchingResDTO.MatchingStatusRes getMyProposeStatus(Member pm, Long projectId, Long memberId) {
        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.PROJECT_NOT_FOUND));

        // 2. PM 권한 검증: 본인 프로젝트인지 확인
        if (!project.isOwnedBy(pm)) {
            throw new MatchingException(MatchingErrorCode.NOT_PROJECT_OWNER);
        }

        // 3. 대상 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            throw new MemberException(MemberErrorCode.NOT_FOUND);
        }

        // 4. PROPOSE 타입, CANCELLED 제외하고 매칭 조회
        Optional<Matching> matchingOpt = matchingRepository
                .findByProjectIdAndMemberIdAndTypeAndStatusNot(
                        projectId, memberId, MatchingType.PROPOSE, MatchingStatus.CANCELLED);

        // 5. 매칭 없으면 exists=false 응답 반환
        return matchingOpt
                .map(MatchingConverter::toMatchingStatusRes)
                .orElse(MatchingResDTO.MatchingStatusRes.notFound(projectId));
    }
}
