package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.security.ClerkPrincipal;

public interface MemberWithdrawalQueryService {

    WithdrawalResDTO.PreviewDTO getPreview(Member member, ClerkPrincipal principal);
}
