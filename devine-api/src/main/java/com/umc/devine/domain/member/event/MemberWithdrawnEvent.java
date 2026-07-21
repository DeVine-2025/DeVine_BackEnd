package com.umc.devine.domain.member.event;

import lombok.Builder;
import lombok.Getter;

/**
 * 회원 탈퇴 처리(soft delete + 익명화) 직후 발행되는 이벤트.
 * <p>커밋 후 리스너에서 Clerk 사용자 삭제 등 외부 시스템 정리 작업을 수행한다.
 * <p>익명화로 인해 Member 엔티티의 clerkId는 이미 마스킹되어 있으므로,
 * 외부 호출에 사용할 원본 clerkId를 이벤트에 담아 전달한다.
 */
@Getter
@Builder
public class MemberWithdrawnEvent {

    private final Long memberId;
    private final String originalClerkId;
}
