package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.converter.MatchingConverter;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorReason;
import com.umc.devine.domain.project.helper.MatchingNotificationHelper;
import com.umc.devine.domain.project.helper.ProjectLifecycleHelper;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.project.validator.MatchingValidator;
import jakarta.persistence.EntityManager;
import com.umc.devine.domain.member.enums.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingCommandServiceImpl implements MatchingCommandService {

    private final MatchingRepository matchingRepository;
    private final ProjectRepository projectRepository;
    private final ProjectRequirementMemberRepository requirementMemberRepository;
    private final MemberRepository memberRepository;
    private final MatchingValidator matchingValidator;
    private final MatchingNotificationHelper notificationHelper;
    private final ProjectLifecycleHelper lifecycleHelper;
    private final EntityManager entityManager;

    @Override
    public MatchingResDTO.ProposeResDTO applyToProject(Member current, Long projectId, ProjectPart part) {
        Member member = lockActiveMember(current.getId());
        Project project = getProject(projectId);

        matchingValidator.validateForApply(member, project);

        Matching matching = matchingRepository.save(
                MatchingConverter.toMatching(project, member, MatchingType.APPLY, part, null)
        );

        notificationHelper.notifyApply(matching);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO cancelApplication(Member member, Long projectId) {
        Project project = getProject(projectId);

        Matching matching = matchingRepository.findByProjectAndMemberAndMatchingTypeAndStatusNot(
                        project, member, MatchingType.APPLY, MatchingStatus.CANCELLED)
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.MATCHING_NOT_FOUND));

        matching.cancel();
        lifecycleHelper.closeIfDeadlinePassedAndNoPending(project);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO proposeToMember(Member pmMember, String targetNickname, Long projectId, ProjectPart part, String content) {
        Member targetMember = lockActiveMember(getMember(targetNickname).getId());
        Project project = getProject(projectId);

        matchingValidator.validateForPropose(pmMember, targetMember, project);

        Matching matching = matchingRepository.save(
                MatchingConverter.toMatching(project, targetMember, MatchingType.PROPOSE, part, content)
        );

        notificationHelper.notifyPropose(pmMember, matching);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO respondToApplication(Member pm, Long matchingId, MatchingDecision decision) {
        Matching matching = getMatching(matchingId);

        matchingValidator.validateForApplicationResponse(pm, matching, decision);

        matching.applyDecision(decision);

        if (decision == MatchingDecision.ACCEPT) {
            incrementCurrentCount(matching);
        }

        notificationHelper.notifyApplicationDecision(matching, decision);
        lifecycleHelper.closeIfDeadlinePassedAndNoPending(matching.getProject());

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO respondToProposal(Member developer, Long matchingId, MatchingDecision decision) {
        Matching matching = getMatching(matchingId);

        matchingValidator.validateForProposalResponse(developer, matching, decision);

        matching.applyDecision(decision);

        if (decision == MatchingDecision.ACCEPT) {
            incrementCurrentCount(matching);
        }

        notificationHelper.notifyProposalDecision(matching, decision);
        lifecycleHelper.closeIfDeadlinePassedAndNoPending(matching.getProject());

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO cancelProposal(Member pm, Long matchingId) {
        Matching matching = getMatching(matchingId);

        if (!matching.isProposeType()) {
            throw new MatchingException(MatchingErrorReason.PROPOSE_MATCHING_NOT_FOUND);
        }
        if (!matching.getProject().isOwnedBy(pm)) {
            throw new MatchingException(MatchingErrorReason.NOT_PROPOSAL_OWNER);
        }

        matching.cancel();
        notificationHelper.notifyProposalCancelled(matching);
        lifecycleHelper.closeIfDeadlinePassedAndNoPending(matching.getProject());

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO updateApplicationPart(Member member, Long projectId, ProjectPart part) {
        Project project = getProject(projectId);

        Matching matching = matchingRepository.findByProjectAndMemberAndMatchingTypeAndStatusNot(
                        project, member, MatchingType.APPLY, MatchingStatus.CANCELLED)
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.MATCHING_NOT_FOUND));

        matching.changePart(part);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    private Project getProject(Long projectId) {
        return projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.PROJECT_NOT_FOUND));
    }

    private Member lockActiveMember(Long memberId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.MEMBER_NOT_FOUND));
        entityManager.refresh(member);
        if (member.getUsed() != MemberStatus.ACTIVE) {
            throw new MatchingException(MatchingErrorReason.MEMBER_NOT_FOUND);
        }
        return member;
    }

    private Member getMember(String nickname) {
        return memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.MEMBER_NOT_FOUND));
    }

    private Matching getMatching(Long matchingId) {
        // 프로젝트를 먼저 잠그고 매칭을 읽어, 같은 매칭을 동시에 수락·취소해도 뒤 요청이 앞 요청의 결과를 보고 판단하게 한다
        Long projectId = matchingRepository.findProjectIdById(matchingId)
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.MATCHING_NOT_FOUND));
        projectRepository.findByIdForUpdate(projectId);
        return matchingRepository.findByIdWithDetails(matchingId)
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.MATCHING_NOT_FOUND));
    }

    private void incrementCurrentCount(Matching matching) {
        if (matching.getPart() == null) {
            throw new MatchingException(MatchingErrorReason.PART_REQUIRED);
        }
        ProjectRequirementMember requirement = requirementMemberRepository
                .findByProjectAndPart(matching.getProject(), matching.getPart())
                .orElseThrow(() -> new MatchingException(MatchingErrorReason.INVALID_PART));
        requirement.incrementCurrentCount();

        // 모든 파트 모집 완료 시 자동으로 진행중으로 변경
        Project project = matching.getProject();
        if (project.isRecruiting() && project.isAllRequirementsFulfilled()) {
            project.startProgress();
        }
    }
}
