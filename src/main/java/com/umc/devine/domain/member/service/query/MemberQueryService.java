package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberResDTO;

public interface MemberQueryService {
    MemberResDTO.MemberDetailDTO findMemberById(Long memberId);
}
