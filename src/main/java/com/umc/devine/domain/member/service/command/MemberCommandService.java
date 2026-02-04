package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.auth.ClerkPrincipal;

public interface MemberCommandService {
    MemberResDTO.SignupResultDTO signup(ClerkPrincipal principal, MemberReqDTO.SignupDTO dto);
    MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto);
    TechstackResDTO.DevTechstackListDTO addMemberTechstacks(Member member, MemberReqDTO.AddTechstackDTO dto);
    TechstackResDTO.DevTechstackListDTO removeMemberTechstacks(Member member, MemberReqDTO.RemoveTechstackDTO dto);
}
