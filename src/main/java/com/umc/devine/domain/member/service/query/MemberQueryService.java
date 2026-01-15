package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;

public interface MemberQueryService {
    MemberResDTO.MemberDetailDTO findMemberById(Long memberId);
    ProjectResDTO.ProjectListDTO findMyProjects(Long memberId);
}
