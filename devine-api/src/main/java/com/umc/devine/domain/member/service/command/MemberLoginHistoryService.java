package com.umc.devine.domain.member.service.command;

public interface MemberLoginHistoryService {

    /**
     * 최근 일정 시간 이내 로그인 기록이 없을 때만 새 로그인 이력을 기록한다(디바운스).
     */
    void recordLoginIfNeeded(Long memberId);
}
