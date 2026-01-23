package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.converter.MatchingConverter;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorCode;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingCommandServiceImpl implements MatchingCommandService {

    private final MatchingRepository matchingRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;

    @Override
    public MatchingResDTO.ProposeResDTO applyToProject(Long memberId, Long projectId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MEMBER_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.PROJECT_NOT_FOUND));

        // 개발자만 지원 가능
        if (member.getMainType() != MemberMainType.DEVELOPER) {
            throw new MatchingException(MatchingErrorCode.INVALID_MEMBER_TYPE_FOR_APPLY);
        }

        // 모집 중인 프로젝트만 지원 가능
        if (project.getStatus() != ProjectStatus.RECRUITING) {
            throw new MatchingException(MatchingErrorCode.PROJECT_NOT_RECRUITING);
        }

        // 본인 프로젝트에 지원 불가
        if (project.getMember().getId().equals(memberId)) {
            throw new MatchingException(MatchingErrorCode.CANNOT_APPLY_OWN_PROJECT);
        }

        // 중복 지원 체크 (취소된 매칭 제외)
        if (matchingRepository.existsByProjectAndMemberAndMatchingTypeAndStatusNot(project, member, MatchingType.APPLY, MatchingStatus.CANCELLED)) {
            throw new MatchingException(MatchingErrorCode.ALREADY_APPLIED);
        }

        Matching matching = MatchingConverter.toMatching(project, member, MatchingType.APPLY);
        Matching savedMatching = matchingRepository.save(matching);

        return MatchingConverter.toMatchingResDTO(savedMatching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO cancelApplication(Long memberId, Long projectId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MEMBER_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.PROJECT_NOT_FOUND));

        Matching matching = matchingRepository.findByProjectAndMemberAndMatchingType(project, member, MatchingType.APPLY)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_NOT_FOUND));

        // 이미 취소된 매칭인지 확인
        if (matching.getStatus() == MatchingStatus.CANCELLED) {
            throw new MatchingException(MatchingErrorCode.ALREADY_CANCELLED);
        }

        matching.cancel();

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO proposeToMember(Long pmMemberId, String targetNickname, Long projectId) {
        Member pmMember = memberRepository.findById(pmMemberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MEMBER_NOT_FOUND));

        Member targetMember = memberRepository.findByNickname(targetNickname)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MEMBER_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.PROJECT_NOT_FOUND));

        // PM만 제안 가능
        if (pmMember.getMainType() != MemberMainType.PM) {
            throw new MatchingException(MatchingErrorCode.INVALID_MEMBER_TYPE_FOR_PROPOSE);
        }

        // 본인 프로젝트만 제안 가능
        if (!project.getMember().getId().equals(pmMemberId)) {
            throw new MatchingException(MatchingErrorCode.NOT_PROJECT_OWNER);
        }

        // 대상이 개발자여야 함
        if (targetMember.getMainType() != MemberMainType.DEVELOPER) {
            throw new MatchingException(MatchingErrorCode.TARGET_NOT_DEVELOPER);
        }

        // 모집 중인 프로젝트만 제안 가능
        if (project.getStatus() != ProjectStatus.RECRUITING) {
            throw new MatchingException(MatchingErrorCode.PROJECT_NOT_RECRUITING);
        }

        // 중복 제안 체크 (취소된 매칭 제외)
        if (matchingRepository.existsByProjectAndMemberAndMatchingTypeAndStatusNot(project, targetMember, MatchingType.PROPOSE, MatchingStatus.CANCELLED)) {
            throw new MatchingException(MatchingErrorCode.ALREADY_PROPOSED);
        }

        Matching matching = MatchingConverter.toMatching(project, targetMember, MatchingType.PROPOSE);
        Matching savedMatching = matchingRepository.save(matching);

        return MatchingConverter.toMatchingResDTO(savedMatching);
    }
}
