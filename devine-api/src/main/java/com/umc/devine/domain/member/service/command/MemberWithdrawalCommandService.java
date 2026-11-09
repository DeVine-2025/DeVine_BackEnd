package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface MemberWithdrawalCommandService {

    WithdrawalResDTO.CancelPendingResultDTO cancelPendingMatchings(Member member);

    /** 외부 작업(Clerk, S3, 메일)은 아웃박스에 기록하고 커밋 뒤 실행한다. */
    WithdrawalResDTO.WithdrawResultDTO withdraw(Member member, WithdrawalReqDTO.WithdrawReq request);

    /** 컨트롤러는 {@link MemberWithdrawalFacade}를 통해 부른다. */
    WithdrawalResDTO.WithdrawResultDTO withdraw(Member member, WithdrawalReqDTO.WithdrawReq request, WithdrawalPreparation preparation);
}
