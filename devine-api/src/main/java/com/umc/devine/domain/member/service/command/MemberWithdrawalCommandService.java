package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface MemberWithdrawalCommandService {

    /** 대기 지원·제안 일괄 취소(FUNC-MEM-002). 상대방에게 알리고, 걸린 프로젝트마다 마감 확인 훅을 부른다. */
    WithdrawalResDTO.CancelPendingResultDTO cancelPendingMatchings(Member member);

    /**
     * 탈퇴 실행(FUNC-MEM-003). 01-1 기능명세서 5장 테이블별 처리를 한 트랜잭션으로 실행하고,
     * 커밋 뒤 할 외부 작업(환불 메일, GitHub 권한 회수·Clerk 삭제·S3 삭제)은 아웃박스에 기록한다.
     */
    WithdrawalResDTO.WithdrawResultDTO withdraw(Member member, WithdrawalReqDTO.WithdrawReq request);
}
