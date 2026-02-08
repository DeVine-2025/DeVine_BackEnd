package com.umc.devine.domain.project.validator;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorCode;
import com.umc.devine.domain.project.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 매칭 관련한 비즈니스 규칙 검증
@Component
@RequiredArgsConstructor
public class MatchingValidator {

    private final MatchingRepository matchingRepository;

    public void validateForApply(Member member, Project project) {
        if (!member.isDeveloper()) {
            throw new MatchingException(MatchingErrorCode.INVALID_MEMBER_TYPE_FOR_APPLY);
        }
        if (!project.isRecruiting()) {
            throw new MatchingException(MatchingErrorCode.PROJECT_NOT_RECRUITING);
        }
        if (project.isOwnedBy(member)) {
            throw new MatchingException(MatchingErrorCode.CANNOT_APPLY_OWN_PROJECT);
        }
        if (existsActiveMatching(project, member, MatchingType.APPLY)) {
            throw new MatchingException(MatchingErrorCode.ALREADY_APPLIED);
        }
    }

    public void validateForPropose(Member pm, Member target, Project project) {
        if (!pm.isPM()) {
            throw new MatchingException(MatchingErrorCode.INVALID_MEMBER_TYPE_FOR_PROPOSE);
        }
        if (!project.isOwnedBy(pm)) {
            throw new MatchingException(MatchingErrorCode.NOT_PROJECT_OWNER);
        }
        if (!target.isDeveloper()) {
            throw new MatchingException(MatchingErrorCode.TARGET_NOT_DEVELOPER);
        }
        if (!project.isRecruiting()) {
            throw new MatchingException(MatchingErrorCode.PROJECT_NOT_RECRUITING);
        }
        if (existsActiveMatching(project, target, MatchingType.PROPOSE)) {
            throw new MatchingException(MatchingErrorCode.ALREADY_PROPOSED);
        }
    }

    public void validateForApplicationResponse(Member pm, Matching matching) {
        if (!pm.isPM()) {
            throw new MatchingException(MatchingErrorCode.INVALID_MEMBER_TYPE_FOR_PROPOSE);
        }
        if (!matching.isApplyType()) {
            throw new MatchingException(MatchingErrorCode.INVALID_MATCHING_TYPE);
        }
        if (!matching.getProject().isOwnedBy(pm)) {
            throw new MatchingException(MatchingErrorCode.NOT_PROJECT_OWNER);
        }
    }

    public void validateForProposalResponse(Member developer, Matching matching) {
        if (!developer.isDeveloper()) {
            throw new MatchingException(MatchingErrorCode.INVALID_MEMBER_TYPE_FOR_APPLY);
        }
        if (!matching.isProposeType()) {
            throw new MatchingException(MatchingErrorCode.INVALID_MATCHING_TYPE);
        }
        if (!matching.isTargetMember(developer)) {
            throw new MatchingException(MatchingErrorCode.NOT_TARGET_MEMBER);
        }
    }

    private boolean existsActiveMatching(Project project, Member member, MatchingType type) {
        return matchingRepository.existsByProjectAndMemberAndMatchingTypeAndStatusNot(
                project, member, type, MatchingStatus.CANCELLED);
    }
}
