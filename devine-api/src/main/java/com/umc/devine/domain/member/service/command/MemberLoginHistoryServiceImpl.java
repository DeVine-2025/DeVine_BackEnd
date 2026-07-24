package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberLoginHistory;
import com.umc.devine.domain.member.repository.MemberLoginHistoryRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
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

    @Override
    public void recordLoginIfNeeded(Long memberId) {
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
