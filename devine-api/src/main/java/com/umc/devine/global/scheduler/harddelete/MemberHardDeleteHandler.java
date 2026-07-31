package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;

/**
 * 회원 하드삭제 배치가 회원 행을 지우기 전에 실행하는 정리 작업 한 단위.
 *
 * 새 회원 연관 테이블이 생기면 이 인터페이스를 구현한 {@code @Component}를 추가하는 것으로 끝난다.
 * {@code MemberHardDeleteScheduler} 자체는 건드릴 필요가 없다. 실행 순서는 {@link org.springframework.core.annotation.Order}로
 * 지정하며, FK가 참조하는 쪽(예: report_embedding)이 참조당하는 쪽(예: dev_report)보다 먼저 실행되어야 한다.
 *
 * 구현체는 "그냥 지운다"가 정답인지 스스로 판단해야 한다. 금전이나 감사 기록이 걸린 테이블이라면
 * 무조건 삭제 대신, 삭제 전 상태 전이(예: 소멸 처리)와 참조 해제만 수행하는 방식을 선택해야 한다
 * (예: {@code CreditRefundRequestHardDeleteHandler}).
 */
public interface MemberHardDeleteHandler {

    void handle(Member member);
}
