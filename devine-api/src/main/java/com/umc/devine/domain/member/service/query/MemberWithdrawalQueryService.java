package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.security.ClerkPrincipal;

public interface MemberWithdrawalQueryService {

    /** 탈퇴 미리보기(FUNC-MEM-001): 대기 건, 지워질 리포트 수, 환불 가능 생성권과 금액. */
    WithdrawalResDTO.PreviewDTO getPreview(Member member, ClerkPrincipal principal);
}
