package com.umc.devine.domain.project.validator;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorReason;
import com.umc.devine.domain.project.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 매칭 관련한 비즈니스 규칙 검증
@Component
@RequiredArgsConstructor
public class MatchingValidator {

    private final MatchingRepository matchingRepository;

    public void validateForApply(Member member, Project project) {
        if (!project.isRecruiting()) {
            throw new MatchingException(MatchingErrorReason.PROJECT_NOT_RECRUITING);
        }
        if (project.isOwnedBy(member)) {
            throw new MatchingException(MatchingErrorReason.CANNOT_APPLY_OWN_PROJECT);
        }
        if (existsActiveMatching(project, member, MatchingType.APPLY)) {
            throw new MatchingException(MatchingErrorReason.ALREADY_APPLIED);
        }
    }

    public void validateForPropose(Member pm, Member target, Project project) {
        if (!project.isOwnedBy(pm)) {
            throw new MatchingException(MatchingErrorReason.NOT_PROJECT_OWNER);
        }
        if (!project.isRecruiting()) {
            throw new MatchingException(MatchingErrorReason.PROJECT_NOT_RECRUITING);
        }
        if (existsActiveMatching(project, target, MatchingType.PROPOSE)) {
            throw new MatchingException(MatchingErrorReason.ALREADY_PROPOSED);
        }
    }

    public void validateForApplicationResponse(Member pm, Matching matching) {
        if (!matching.isPending()) {
            throw new MatchingException(MatchingErrorReason.INVALID_STATUS_TRANSITION);
        }
        if (!matching.isApplyType()) {
            throw new MatchingException(MatchingErrorReason.APPLY_MATCHING_NOT_FOUND);
        }
        if (!matching.getProject().isOwnedBy(pm)) {
            throw new MatchingException(MatchingErrorReason.NOT_PROJECT_OWNER);
        }
    }

    public void validateForProposalResponse(Member developer, Matching matching) {
        if (!matching.isPending()) {
            throw new MatchingException(MatchingErrorReason.INVALID_STATUS_TRANSITION);
        }
        if (!matching.isProposeType()) {
            throw new MatchingException(MatchingErrorReason.PROPOSE_MATCHING_NOT_FOUND);
        }
        if (!matching.isTargetMember(developer)) {
            throw new MatchingException(MatchingErrorReason.NOT_TARGET_MEMBER);
        }
    }

    private boolean existsActiveMatching(Project project, Member member, MatchingType type) {
        return matchingRepository.existsByProjectAndMemberAndMatchingTypeAndStatusNot(
                project, member, type, MatchingStatus.CANCELLED);
    }
}
