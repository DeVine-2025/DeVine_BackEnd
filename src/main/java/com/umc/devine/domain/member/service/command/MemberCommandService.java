package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;

public interface MemberCommandService {
    MemberResDTO.MemberDetailDTO updateMember(Long memberId, MemberReqDTO.UpdateMemberDTO dto);
}
