package com.umc.devine.domain.member.event;

import lombok.Builder;
import lombok.Getter;

/**
 * 회원 자진 탈퇴가 DB 트랜잭션에 커밋된 후, Clerk 계정 삭제를 비동기로 트리거하기 위한 이벤트.
 * originalClerkId는 Member.selfWithdraw()가 익명화하기 전의 실제 Clerk 사용자 ID다.
 */
@Getter
@Builder
public class MemberWithdrawnEvent {

    private final Long memberId;
    private final String originalClerkId;
}
