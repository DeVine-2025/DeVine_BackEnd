package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.member.converter.MemberConverter;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.exception.code.ProjectErrorCode;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.techstack.converter.TechstackConverter;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.infrastructure.github.GitHubService;
import com.umc.devine.infrastructure.github.dto.GitHubContributionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final DevTechstackRepository devTechstackRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final ContactRepository contactRepository;
    private final TermsRepository termsRepository;
    private final GitHubService gitHubService;

    @Override
    public MemberResDTO.TermsListDTO findAllTerms() {
        List<Terms> termsList = termsRepository.findAll();
        return MemberConverter.toTermsListDTO(termsList);
    }

    @Override
    public MemberResDTO.MemberProfileDTO findMemberProfile(Member member) {
        List<MemberCategory> memberCategories = memberCategoryRepository.findAllByMemberWithCategory(member);
        List<Contact> contacts = contactRepository.findAllByMember(member);

        return MemberConverter.toMemberProfileDTO(member, memberCategories, contacts);
    }

    @Override
    public TechstackResDTO.DevTechstackListDTO findMemberTechstacks(Member member) {
        List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(member);
        return TechstackConverter.toDevTechstackListDTO(devTechstacks);
    }

    @Override
    public MemberResDTO.UserProfileDTO findMemberByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(member);

        return MemberConverter.toUserProfileDTO(member, devTechstacks);
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
    public MemberResDTO.ContributionListDTO findMyContributions(Member member, LocalDate from, LocalDate to) {
        List<GitHubContributionDTO> githubContributions = gitHubService.getContributions(member.getClerkId(), from, to);

        List<MemberResDTO.ContributionDTO> contributions = githubContributions.stream()
                .map(gc -> MemberResDTO.ContributionDTO.builder()
                        .date(gc.getDate())
                        .count(gc.getContributionCount())
                        .build())
                .toList();

        return MemberResDTO.ContributionListDTO.builder()
                .contributionList(contributions)
                .build();
    }

    @Override
    public MemberResDTO.ContributionListDTO findContributionsByNickname(String nickname, LocalDate from, LocalDate to) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        List<GitHubContributionDTO> githubContributions = gitHubService.getContributionsByUsername(member.getGithubUsername(), from, to);

        List<MemberResDTO.ContributionDTO> contributions = githubContributions.stream()
                .map(gc -> MemberResDTO.ContributionDTO.builder()
                        .date(gc.getDate())
                        .count(gc.getContributionCount())
                        .build())
                .toList();

        return MemberResDTO.ContributionListDTO.builder()
                .contributionList(contributions)
                .build();
    }

    @Override
    public PagedResponse<MemberResDTO.DeveloperDTO> findAllDevelopers(Member member, MemberReqDTO.RecommendDeveloperDTO dto) {
        Project project = projectRepository.findByIdWithCategory(dto.projectId())
                .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));

        CategoryGenre category = project.getCategory().getGenre();

        Page<Member> developerPage = memberRepository.findDevelopersByFilters(
                List.of(category),
                null,
                dto.toPageable()
        );

        List<Member> developers = developerPage.getContent();
        if (developers.isEmpty()) {
            return PagedResponse.of(developerPage, Collections.emptyList());
        }

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(developers);
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
        List<Member> developers = memberRepository.findAllPublicMembers(
                org.springframework.data.domain.PageRequest.of(0, limit)
        );

        if (developers.isEmpty()) {
            return Collections.emptyList();
        }

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(developers);
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
        List<CategoryGenre> categories = request.categories() != null && !request.categories().isEmpty()
                ? request.categories() : null;
        List<TechName> techstackNames = request.techstackNames() != null && !request.techstackNames().isEmpty()
                ? request.techstackNames() : null;

        Page<Member> developerPage = memberRepository.findDevelopersByFilters(
                categories,
                techstackNames,
                request.toPageable()
        );

        List<Member> members = developerPage.getContent();
        if (members.isEmpty()) {
            return PagedResponse.of(developerPage, Collections.emptyList());
        }

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(members);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        List<MemberResDTO.UserProfileDTO> developerDTOs = members.stream().map(member -> {
            List<DevTechstack> devTechstacks = techstacksByMemberId.getOrDefault(member.getId(), Collections.emptyList());
            return MemberConverter.toUserProfileDTO(member, devTechstacks);
        }).collect(Collectors.toList());

        return PagedResponse.of(developerPage, developerDTOs);
    }
}
