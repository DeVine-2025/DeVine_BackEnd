package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.dto.PagedResponse;

import java.time.LocalDate;
import java.util.List;

public interface MemberQueryService {
    MemberResDTO.TermsListDTO findAllTerms();
    MemberResDTO.MemberProfileDTO findMemberProfile(Member member);
    TechstackResDTO.DevTechstackListDTO findMemberTechstacks(Member member);
    MemberResDTO.UserProfileDTO findMemberByNickname(String nickname);
    ProjectResDTO.ProjectListDTO findMyProjects(Member member);
    MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname);
    MemberResDTO.ContributionListDTO findMyContributions(Member member, LocalDate from, LocalDate to);
    MemberResDTO.ContributionListDTO findContributionsByNickname(String nickname, LocalDate from, LocalDate to);
    PagedResponse<MemberResDTO.DeveloperDTO> findAllDevelopers(Member member, MemberReqDTO.RecommendDeveloperDTO dto);
    List<MemberResDTO.DeveloperDTO> findAllDevelopersPreview(Member member, int limit);
    PagedResponse<MemberResDTO.UserProfileDTO> searchDevelopers(MemberReqDTO.SearchDeveloperDTO dto);
}
