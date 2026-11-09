package com.umc.devine.domain.member.helper;

import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 탈퇴할 회원이 개발자 쪽(matching.member)이나 PM 쪽(project.member)으로 걸린 대기 건 */
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
