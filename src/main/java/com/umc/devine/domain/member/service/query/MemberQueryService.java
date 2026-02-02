package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.dto.PagedResponse;

import java.util.List;

public interface MemberQueryService {
    MemberResDTO.MemberProfileDTO findMemberProfile(Member member);
    TechstackResDTO.DevTechstackListDTO findMemberTechstacks(Member member);
    MemberResDTO.UserProfileDTO findMemberByNickname(String nickname);
    ProjectResDTO.ProjectListDTO findMyProjects(Member member);
    MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname);
    DevReportResDTO.ReportListDTO findMyReports(Member member);
    DevReportResDTO.ReportListDTO findReportsByNickname(String nickname);
    MemberResDTO.ContributionListDTO findContributionsById(Long memberId);
    MemberResDTO.ContributionListDTO findContributionsByNickname(String nickname);
    PagedResponse<MemberResDTO.DeveloperDTO> findAllDevelopers(Member member, MemberReqDTO.RecommendDeveloperDTO dto);
    List<MemberResDTO.DeveloperDTO> findAllDevelopersPreview(Member member, int limit);
    PagedResponse<MemberResDTO.UserProfileDTO> searchDevelopers(MemberReqDTO.SearchDeveloperDTO dto);
}
