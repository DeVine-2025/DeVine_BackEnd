package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.member.converter.MemberConverter;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.project.converter.ProjectConverter;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;
import com.umc.devine.domain.project.repository.ProjectImageRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.techstack.converter.DevReportConverter;
import com.umc.devine.domain.techstack.converter.TechstackConverter;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.entity.mapping.ReportTechstack;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.ReportTechstackRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final DevTechstackRepository devTechstackRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final DevReportRepository devReportRepository;
    private final ReportTechstackRepository reportTechstackRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final ContactRepository contactRepository;
    private final TermsRepository termsRepository;

    @Override
    public MemberResDTO.TermsListDTO findAllTerms() {
        List<Terms> termsList = termsRepository.findAll();
        return MemberConverter.toTermsListDTO(termsList);
    }

    @Override
    public MemberResDTO.MemberProfileDTO findMemberProfile(Member member) {
        List<MemberCategory> memberCategories = memberCategoryRepository.findAllByMember(member);
        List<Contact> contacts = contactRepository.findAllByMember(member);

        return MemberConverter.toMemberProfileDTO(member, memberCategories, contacts);
    }

    @Override
    public TechstackResDTO.DevTechstackListDTO findMemberTechstacks(Member member) {
        List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMember(member);
        return TechstackConverter.toDevTechstackListDTO(devTechstacks);
    }

    @Override
    public MemberResDTO.UserProfileDTO findMemberByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMember(member);

        return MemberConverter.toUserProfileDTO(member, devTechstacks);
    }

    @Override
    public ProjectResDTO.ProjectListDTO findMyProjects(Member member) {
        List<Project> projects = projectRepository.findByMember(member);

        if (projects.isEmpty()) {
            return ProjectConverter.toProjectList(Collections.emptyList());
        }

        List<ProjectImage> allImages = projectImageRepository.findAllByProjectIn(projects);
        Map<Long, List<ProjectImage>> imagesByProjectId = allImages.stream()
                .collect(Collectors.groupingBy(image -> image.getProject().getId()));

        List<ProjectResDTO.ProjectDetailDTO> projectList = projects.stream().map(project -> {
            List<ProjectImage> images = imagesByProjectId.getOrDefault(project.getId(), Collections.emptyList());
            return ProjectConverter.toProjectDetail(project, images);
        }).collect(Collectors.toList());

        return ProjectConverter.toProjectList(projectList);
    }

    @Override
    public MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname) {
        boolean isDuplicate = memberRepository.existsByNickname(nickname);
        return MemberResDTO.NicknameDuplicateDTO.builder()
                .nickname(nickname)
                .isDuplicate(isDuplicate)
                .build();
    }

    @Override
    public DevReportResDTO.ReportListDTO findMyReports(Member member) {
        List<GitRepoUrl> gitRepoUrls = gitRepoUrlRepository.findAllByMember(member);
        List<DevReport> devReports = devReportRepository.findAllByGitRepoUrlIn(gitRepoUrls);

        if (devReports.isEmpty()) {
            return DevReportConverter.toReportListDTO(Collections.emptyList());
        }

        List<ReportTechstack> allReportTechstacks = reportTechstackRepository.findAllByDevReportIn(devReports);
        Map<Long, List<ReportTechstack>> techstacksByReportId = allReportTechstacks.stream()
                .collect(Collectors.groupingBy(rt -> rt.getDevReport().getId()));

        List<DevReportResDTO.ReportDTO> reportDTOs = devReports.stream()
                .map(report -> {
                    List<ReportTechstack> reportTechstacks = techstacksByReportId.getOrDefault(report.getId(), Collections.emptyList());
                    return DevReportConverter.toReportDTO(report, reportTechstacks);
                })
                .collect(Collectors.toList());

        return DevReportConverter.toReportListDTO(reportDTOs);
    }

    @Override
    public DevReportResDTO.ReportListDTO findReportsByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        List<GitRepoUrl> gitRepoUrls = gitRepoUrlRepository.findAllByMember(member);
        List<DevReport> devReports = devReportRepository.findAllByGitRepoUrlIn(gitRepoUrls);

        if (devReports.isEmpty()) {
            return DevReportConverter.toReportListDTO(Collections.emptyList());
        }

        List<ReportTechstack> allReportTechstacks = reportTechstackRepository.findAllByDevReportIn(devReports);
        Map<Long, List<ReportTechstack>> techstacksByReportId = allReportTechstacks.stream()
                .collect(Collectors.groupingBy(rt -> rt.getDevReport().getId()));

        List<DevReportResDTO.ReportDTO> reportDTOs = devReports.stream()
                .map(report -> {
                    List<ReportTechstack> reportTechstacks = techstacksByReportId.getOrDefault(report.getId(), Collections.emptyList());
                    return DevReportConverter.toReportDTO(report, reportTechstacks);
                })
                .collect(Collectors.toList());

        return DevReportConverter.toReportListDTO(reportDTOs);
    }

    @Override
    public MemberResDTO.ContributionListDTO findContributionsById(Long memberId) {
        // TODO: GITHUB api 연동하기 및 방식 정하기
        // Mock data : https://github.com/strfunctionk/GithubAPITest 참고
        List<MemberResDTO.ContributionDTO> contributions = List.of(
                MemberResDTO.ContributionDTO.builder().date("2024-01-01").count(3).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-02").count(5).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-03").count(0).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-04").count(2).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-05").count(7).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-06").count(1).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-07").count(4).build()
        );

        return MemberResDTO.ContributionListDTO.builder()
                .contributionList(contributions)
                .build();
    }

    @Override
    public MemberResDTO.ContributionListDTO findContributionsByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        // TODO: GITHUB api 연동하기 및 방식 정하기
        // Mock data : https://github.com/strfunctionk/GithubAPITest 참고
        List<MemberResDTO.ContributionDTO> contributions = List.of(
                MemberResDTO.ContributionDTO.builder().date("2024-01-01").count(3).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-02").count(5).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-03").count(0).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-04").count(2).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-05").count(7).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-06").count(1).build(),
                MemberResDTO.ContributionDTO.builder().date("2024-01-07").count(4).build()
        );

        return MemberResDTO.ContributionListDTO.builder()
                .contributionList(contributions)
                .build();
    }

    @Override
    public PagedResponse<MemberResDTO.DeveloperDTO> findAllDevelopers(Member member, MemberReqDTO.RecommendDeveloperDTO dto) {
        // TODO: projectIds를 활용한 추천 로직 구현 (dto.projectIds())
        Page<Member> developerPage = memberRepository.findDevelopersByFilters(
                MemberMainType.DEVELOPER,
                dto.category(),
                dto.techGenre(),
                dto.techstackName(),
                dto.toPageable()
        );

        List<Member> developers = developerPage.getContent();
        if (developers.isEmpty()) {
            return PagedResponse.of(developerPage, Collections.emptyList());
        }

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberIn(developers);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        List<MemberResDTO.DeveloperDTO> developerDTOs = developers.stream()
                .map(developer -> {
                    List<DevTechstack> devTechstacks = techstacksByMemberId.getOrDefault(developer.getId(), Collections.emptyList());
                    return MemberConverter.toDeveloperDTO(developer, devTechstacks);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(developerPage, developerDTOs);
    }


    @Override
    public List<MemberResDTO.DeveloperDTO> findAllDevelopersPreview(Member member, int limit) {
        // TODO: member를 활용한 추천 로직 구현
        List<Member> developers = memberRepository.findAllByMainType(
                MemberMainType.DEVELOPER,
                org.springframework.data.domain.PageRequest.of(0, limit)
        );

        if (developers.isEmpty()) {
            return Collections.emptyList();
        }

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberIn(developers);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        return developers.stream()
                .map(developer -> {
                    List<DevTechstack> devTechstacks = techstacksByMemberId.getOrDefault(developer.getId(), Collections.emptyList());
                    return MemberConverter.toDeveloperDTO(developer, devTechstacks);
                })
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<MemberResDTO.UserProfileDTO> searchDevelopers(MemberReqDTO.SearchDeveloperDTO request) {
        Page<Member> developerPage = memberRepository.findDevelopersByFilters(
                MemberMainType.DEVELOPER,
                request.category(),
                request.techGenre(),
                request.techstackName(),
                request.toPageable()
        );

        List<Member> members = developerPage.getContent();
        if (members.isEmpty()) {
            return PagedResponse.of(developerPage, Collections.emptyList());
        }

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberIn(members);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        List<MemberResDTO.UserProfileDTO> developerDTOs = members.stream().map(member -> {
            List<DevTechstack> devTechstacks = techstacksByMemberId.getOrDefault(member.getId(), Collections.emptyList());
            return MemberConverter.toUserProfileDTO(member, devTechstacks);
        }).collect(Collectors.toList());

        return PagedResponse.of(developerPage, developerDTOs);
    }
}
