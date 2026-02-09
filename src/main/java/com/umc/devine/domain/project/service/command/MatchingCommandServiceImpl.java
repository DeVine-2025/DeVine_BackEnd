package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.converter.MatchingConverter;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorCode;
import com.umc.devine.domain.project.helper.MatchingNotificationHelper;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.validator.MatchingValidator;
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
    private final MatchingValidator matchingValidator;
    private final MatchingNotificationHelper notificationHelper;

    @Override
    public MatchingResDTO.ProposeResDTO applyToProject(Member member, Long projectId) {
        Project project = getProject(projectId);

        matchingValidator.validateForApply(member, project);

        Matching matching = matchingRepository.save(
                MatchingConverter.toMatching(project, member, MatchingType.APPLY)
        );

        notificationHelper.notifyApply(matching);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO cancelApplication(Member member, Long projectId) {
        Project project = getProject(projectId);

        Matching matching = matchingRepository.findByProjectAndMemberAndMatchingTypeAndStatusNot(
                        project, member, MatchingType.APPLY, MatchingStatus.CANCELLED)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_NOT_FOUND));

        matching.cancel();

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO proposeToMember(Member pmMember, String targetNickname, Long projectId) {
        Member targetMember = getMember(targetNickname);
        Project project = getProject(projectId);

        matchingValidator.validateForPropose(pmMember, targetMember, project);

        Matching matching = matchingRepository.save(
                MatchingConverter.toMatching(project, targetMember, MatchingType.PROPOSE)
        );

        notificationHelper.notifyPropose(pmMember, matching);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO respondToApplication(Member pm, Long matchingId, MatchingDecision decision) {
        Matching matching = getMatching(matchingId);

        matchingValidator.validateForApplicationResponse(pm, matching);

        matching.applyDecision(decision);

        notificationHelper.notifyApplicationDecision(matching, decision);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    @Override
    public MatchingResDTO.ProposeResDTO respondToProposal(Member developer, Long matchingId, MatchingDecision decision) {
        Matching matching = getMatching(matchingId);

        matchingValidator.validateForProposalResponse(developer, matching);

        matching.applyDecision(decision);

        notificationHelper.notifyProposalDecision(matching, decision);

        return MatchingConverter.toMatchingResDTO(matching);
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.PROJECT_NOT_FOUND));
    }

    private Member getMember(String nickname) {
        return memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MEMBER_NOT_FOUND));
    }

    private Matching getMatching(Long matchingId) {
        return matchingRepository.findByIdWithDetails(matchingId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_NOT_FOUND));
    }
}
