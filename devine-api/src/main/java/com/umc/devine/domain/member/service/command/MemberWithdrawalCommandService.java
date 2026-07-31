package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface MemberWithdrawalCommandService {

    MemberResDTO.WithdrawalPreviewDTO getWithdrawalPreview(Member member);

    MemberResDTO.WithdrawalResultDTO selfWithdraw(Member member, MemberReqDTO.SelfWithdrawReq request);
}
