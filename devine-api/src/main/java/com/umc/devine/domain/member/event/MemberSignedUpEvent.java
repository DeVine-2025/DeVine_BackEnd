package com.umc.devine.domain.member.event;

import com.umc.devine.domain.member.entity.Member;

/**
 * 회원가입 완료 시 발행되는 도메인 이벤트
 * Member → Ticket 간 결합을 제거하기 위해 사용
 */
public record MemberSignedUpEvent(Member member) {
}
