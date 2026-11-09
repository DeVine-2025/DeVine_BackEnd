package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface MemberQueryService {
    MemberResDTO.TermsListDTO findAllTerms();
    MemberResDTO.TermsDTO findTermById(Long id);
    MemberResDTO.MemberProfileDTO findMemberProfile(Member member);
    TechstackResDTO.DevTechstackListDTO findMemberTechstacks(Member member);
    TechstackResDTO.DevTechstackListDTO findTechstacksByNickname(Member viewer, String nickname);
    MemberResDTO.MemberProfileDTO findMemberByNickname(Member viewer, String nickname);
    MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname);
    MemberResDTO.ContributionListDTO findMyContributions(Member member, LocalDate from, LocalDate to);
    MemberResDTO.ContributionListDTO findContributionsByNickname(Member viewer, String nickname, LocalDate from, LocalDate to);
    PagedResponse<MemberResDTO.RecommendedDeveloperDTO> findRecommendedDevelopers(Member member, MemberReqDTO.RecommendDeveloperDTO dto);
    List<MemberResDTO.RecommendedDeveloperDTO> findRecommendedDevelopersPreview(Member member, Long projectId, int limit);
    PagedResponse<MemberResDTO.MemberListItemDTO> searchDevelopers(Member viewer, MemberReqDTO.SearchDeveloperDTO dto);
    ProjectResDTO.MyProjectsRes findProjectsByNickname(Member viewer, String nickname, List<ProjectStatus> statuses, Pageable pageable);
    PagedResponse<MemberResDTO.GitRepoDTO> findReposByNickname(Member viewer, String nickname, Pageable pageable);
}
