package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberLoginHistory;
import com.umc.devine.domain.member.repository.MemberLoginHistoryRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberLoginHistoryServiceImpl implements MemberLoginHistoryService {

    private static final int DEBOUNCE_MINUTES = 10;

    private final MemberLoginHistoryRepository memberLoginHistoryRepository;
    private final MemberRepository memberRepository;
    private final EntityManager entityManager;

    @Override
    public void recordLoginIfNeeded(Long memberId) {
        // 동시 로그인 요청 간 확인-후-저장 경합을 막기 위해 회원 단위로 직렬화한다.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:memberId)")
                .setParameter("memberId", memberId)
                .getSingleResult();

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(DEBOUNCE_MINUTES);
        if (memberLoginHistoryRepository.existsByMemberIdAndLoginAtAfter(memberId, threshold)) {
            return;
        }

        Member member = memberRepository.getReferenceById(memberId);
        memberLoginHistoryRepository.save(MemberLoginHistory.builder()
                .member(member)
                .loginAt(LocalDateTime.now())
                .build());
    }
}
