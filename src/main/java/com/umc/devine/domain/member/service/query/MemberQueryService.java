package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;

public interface MemberQueryService {
    MemberResDTO.MemberDetailDTO findMemberById(Long memberId);
    MemberResDTO.UserProfileDTO findMemberByNickname(String nickname);
    ProjectResDTO.ProjectListDTO findMyProjects(Long memberId);
    MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname);
    DevReportResDTO.ReportListDTO findMyReports(Long memberId);
    DevReportResDTO.ReportListDTO findReportsByNickname(String nickname);
    MemberResDTO.ContributionListDTO findContributionsById(Long memberId);
    MemberResDTO.ContributionListDTO findContributionsByNickname(String nickname);
}
