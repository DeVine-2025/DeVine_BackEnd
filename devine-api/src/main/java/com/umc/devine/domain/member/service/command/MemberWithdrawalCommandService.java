package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface MemberWithdrawalCommandService {

    WithdrawalResDTO.CancelPendingResultDTO cancelPendingMatchings(Member member);

    WithdrawalResDTO.WithdrawResultDTO withdraw(Member member, WithdrawalReqDTO.WithdrawReq request);

    WithdrawalResDTO.WithdrawResultDTO withdraw(Member member, WithdrawalReqDTO.WithdrawReq request, WithdrawalPreparation preparation);
}
