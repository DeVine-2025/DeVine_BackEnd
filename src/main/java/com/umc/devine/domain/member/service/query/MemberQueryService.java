package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.dto.PagedResponse;

public interface MemberQueryService {
    MemberResDTO.MemberProfileDTO findMemberProfile(Member member);
    TechstackResDTO.DevTechstackListDTO findMemberTechstacks(Member member);
    MemberResDTO.UserProfileDTO findMemberByNickname(String nickname);
    ProjectResDTO.ProjectListDTO findMyProjects(Member member);
    MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname);
    DevReportResDTO.ReportListDTO findMyReports(Long memberId);
    DevReportResDTO.ReportListDTO findReportsByNickname(String nickname);
    MemberResDTO.ContributionListDTO findContributionsById(Long memberId);
    MemberResDTO.ContributionListDTO findContributionsByNickname(String nickname);
    MemberResDTO.DeveloperListDTO findAllDevelopers(Long memberId);
    PagedResponse<MemberResDTO.UserProfileDTO> searchDevelopers(MemberReqDTO.SearchDeveloperDTO dto);
}
