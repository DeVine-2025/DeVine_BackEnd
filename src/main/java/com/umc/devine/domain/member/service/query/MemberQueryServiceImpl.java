package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.auth.exception.AuthException;
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
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
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
import java.util.Set;
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
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;

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
    public TechstackResDTO.DevTechstackListDTO findTechstacksByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(member);
        return TechstackConverter.toDevTechstackListDTO(devTechstacks);
    }

    @Override
    public MemberResDTO.MemberProfileDTO findMemberByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        List<MemberCategory> memberCategories = memberCategoryRepository.findAllByMemberWithCategory(member);
        List<Contact> contacts = contactRepository.findAllByMember(member);

        return MemberConverter.toMemberProfileDTO(member, memberCategories, contacts);
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
    @Transactional
    public MemberResDTO.ContributionListDTO findContributionsByNickname(String nickname, LocalDate from, LocalDate to) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        // GitHub username이 없으면 Clerk API로 조회 시도
        if (member.getGithubUsername() == null || member.getGithubUsername().isBlank()) {
            try {
                Map<String, Object> userInfo = gitHubService.getUserInfo(member.getClerkId());
                String githubUsername = (String) userInfo.get("login");
                member.updateGithubUsername(githubUsername);
            } catch (AuthException e) {
                // GitHub 연동이 안 되어 있으면 에러
                throw new MemberException(MemberErrorCode.GITHUB_USERNAME_NOT_FOUND);
            }
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
    public PagedResponse<MemberResDTO.RecommendedDeveloperDTO> findRecommendedDevelopers(Member member, MemberReqDTO.RecommendDeveloperDTO dto) {
        // projectId가 없으면 빈 배열 반환
        if (dto.projectId() == null) {
            return PagedResponse.empty(dto.toPageable());
        }

        Project project = projectRepository.findByIdWithCategory(dto.projectId())
                .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));

        if (project.getCategory() == null) {
            return PagedResponse.empty(dto.toPageable());
        }
        CategoryGenre category = project.getCategory().getGenre();

        // 프로젝트 요구 기술스택 조회
        List<ProjectRequirementTechstack> projectTechstacks =
                projectRequirementTechstackRepository.findAllByProjectIdWithTechstack(dto.projectId());
        Set<TechName> requiredTechstackNames = projectTechstacks.stream()
                .map(prt -> prt.getTechstack().getName())
                .collect(Collectors.toSet());

        // 해당 카테고리의 개발자 페이지네이션 조회
        Page<Member> developerPage = memberRepository.findDevelopersByFilters(
                List.of(category),
                null,
                dto.toPageable()
        );

        List<Member> developers = developerPage.getContent();
        if (developers.isEmpty()) {
            return PagedResponse.of(developerPage, Collections.emptyList());
        }

        // N+1 방지: 개발자들의 카테고리, 기술스택 한 번에 조회
        List<MemberCategory> allMemberCategories = memberCategoryRepository.findAllByMemberInWithCategory(developers);
        Map<Long, List<MemberCategory>> categoriesByMemberId = allMemberCategories.stream()
                .collect(Collectors.groupingBy(mc -> mc.getMember().getId()));

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(developers);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        List<MemberResDTO.RecommendedDeveloperDTO> developerDTOs = developers.stream()
                .map(developer -> {
                    List<MemberCategory> memberCategories = categoriesByMemberId.getOrDefault(developer.getId(), Collections.emptyList());
                    List<DevTechstack> devTechstacks = techstacksByMemberId.getOrDefault(developer.getId(), Collections.emptyList());

                    // 매칭된 기술스택 계산
                    List<String> matchedTechstacks = devTechstacks.stream()
                            .map(dt -> dt.getTechstack().getName())
                            .filter(requiredTechstackNames::contains)
                            .map(TechName::toString)
                            .collect(Collectors.toList());

                    // 도메인 일치 여부
                    boolean domainMatch = memberCategories.stream()
                            .anyMatch(mc -> mc.getCategory().getGenre() == category);

                    return MemberConverter.toRecommendedDeveloperDTO(
                            developer, memberCategories, devTechstacks,
                            null, null, null, domainMatch, matchedTechstacks
                    );
                })
                .collect(Collectors.toList());

        return PagedResponse.of(developerPage, developerDTOs);
    }

    @Override
    public List<MemberResDTO.RecommendedDeveloperDTO> findRecommendedDevelopersPreview(Member member, Long projectId, int limit) {
        // projectId가 없으면 빈 배열 반환
        if (projectId == null) {
            return Collections.emptyList();
        }

        Project project = projectRepository.findByIdWithCategory(projectId)
                .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));

        if (project.getCategory() == null) {
            return Collections.emptyList();
        }
        CategoryGenre category = project.getCategory().getGenre();

        // 프로젝트 요구 기술스택 조회
        List<ProjectRequirementTechstack> projectTechstacks =
                projectRequirementTechstackRepository.findAllByProjectIdWithTechstack(projectId);
        Set<TechName> requiredTechstackNames = projectTechstacks.stream()
                .map(prt -> prt.getTechstack().getName())
                .collect(Collectors.toSet());

        // 해당 카테고리의 개발자 조회 (limit 개수만큼)
        List<Member> developers = memberRepository.findDevelopersByFilters(
                List.of(category),
                null,
                org.springframework.data.domain.PageRequest.of(0, limit)
        ).getContent();

        if (developers.isEmpty()) {
            return Collections.emptyList();
        }

        // N+1 방지: 개발자들의 카테고리, 기술스택 한 번에 조회
        List<MemberCategory> allMemberCategories = memberCategoryRepository.findAllByMemberInWithCategory(developers);
        Map<Long, List<MemberCategory>> categoriesByMemberId = allMemberCategories.stream()
                .collect(Collectors.groupingBy(mc -> mc.getMember().getId()));

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(developers);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        return developers.stream()
                .map(developer -> {
                    List<MemberCategory> memberCategories = categoriesByMemberId.getOrDefault(developer.getId(), Collections.emptyList());
                    List<DevTechstack> devTechstacks = techstacksByMemberId.getOrDefault(developer.getId(), Collections.emptyList());

                    // 매칭된 기술스택 계산
                    List<String> matchedTechstacks = devTechstacks.stream()
                            .map(dt -> dt.getTechstack().getName())
                            .filter(requiredTechstackNames::contains)
                            .map(TechName::toString)
                            .collect(Collectors.toList());

                    // 도메인 일치 여부
                    boolean domainMatch = memberCategories.stream()
                            .anyMatch(mc -> mc.getCategory().getGenre() == category);

                    return MemberConverter.toRecommendedDeveloperDTO(
                            developer, memberCategories, devTechstacks,
                            null, null, null, domainMatch, matchedTechstacks
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<MemberResDTO.MemberListItemDTO> searchDevelopers(MemberReqDTO.SearchDeveloperDTO request) {
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

        List<MemberCategory> allMemberCategories = memberCategoryRepository.findAllByMemberInWithCategory(members);
        Map<Long, List<MemberCategory>> categoriesByMemberId = allMemberCategories.stream()
                .collect(Collectors.groupingBy(mc -> mc.getMember().getId()));

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(members);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        List<MemberResDTO.MemberListItemDTO> developerDTOs = members.stream()
                .map(member -> {
                    List<MemberCategory> memberCategories = categoriesByMemberId.getOrDefault(member.getId(), Collections.emptyList());
                    List<DevTechstack> devTechstacks = techstacksByMemberId.getOrDefault(member.getId(), Collections.emptyList());
                    return MemberConverter.toMemberListItemDTO(member, memberCategories, devTechstacks);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(developerPage, developerDTOs);
    }
}
