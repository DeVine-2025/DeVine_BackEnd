package com.umc.devine.domain.member.helper;

import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 탈퇴할 회원이 보냈거나 받은 대기(수락·거절 전) 지원·제안을 찾는다(FUNC-MEM-001 1번).
 * 대기 건 기준은 프로젝트 관리와 같다(status = PENDING).
 *
 * <ul>
 *     <li>개발자 쪽(matching.member = 회원): 내가 보낸 지원, 내가 받은 제안</li>
 *     <li>PM 쪽(project.member = 회원): 내 프로젝트에 들어온 지원, 내가 보낸 제안</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PendingMatchingHelper {

    private final MatchingRepository matchingRepository;

    public List<Matching> findPending(Member member) {
        return matchingRepository.findAllInvolvingMemberByStatus(member, MatchingStatus.PENDING);
    }

    public List<Long> findPendingProjectIds(Member member) {
        return matchingRepository.findProjectIdsInvolvingMemberByStatus(member, MatchingStatus.PENDING);
    }

    /** 회원이 개발자 쪽이면 PM이, PM 쪽이면 개발자가 상대방이다. */
    public static Member counterpartOf(Matching matching, Member member) {
        return isDeveloperSide(matching, member) ? matching.getProject().getMember() : matching.getMember();
    }

    public static WithdrawalResDTO.Direction directionOf(Matching matching, Member member) {
        boolean developerSide = isDeveloperSide(matching, member);
        // 지원은 개발자가 보내고, 제안은 PM이 보낸다
        boolean sent = matching.isApplyType() == developerSide;
        return sent ? WithdrawalResDTO.Direction.SENT : WithdrawalResDTO.Direction.RECEIVED;
    }

    private static boolean isDeveloperSide(Matching matching, Member member) {
        return matching.getMember().getId().equals(member.getId());
    }
}
