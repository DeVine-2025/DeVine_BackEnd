package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;

public interface MemberQueryService {
    MemberResDTO.MemberDetailDTO findMemberById(Long memberId);
    MemberResDTO.UserProfileDTO findMemberByNickname(String nickname);
    ProjectResDTO.ProjectListDTO findMyProjects(Long memberId);
    MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname);
}
