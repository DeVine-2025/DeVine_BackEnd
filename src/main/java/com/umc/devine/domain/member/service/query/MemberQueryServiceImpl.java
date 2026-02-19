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
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRecommendRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.exception.code.ProjectErrorCode;
import com.umc.devine.domain.project.repository.ProjectEmbeddingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.service.query.ProjectQueryService;
import com.umc.devine.domain.techstack.converter.TechstackConverter;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.enums.EmbeddingStatus;
import com.umc.devine.infrastructure.github.GitHubService;
import com.umc.devine.infrastructure.github.dto.GitHubContributionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
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
    private final MemberRecommendRepository memberRecommendRepository;
    private final ProjectEmbeddingRepository projectEmbeddingRepository;
    private final ProjectQueryService projectQueryService;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final DevReportRepository devReportRepository;

    @Override
    public MemberResDTO.TermsListDTO findAllTerms() {
        List<Terms> termsList = termsRepository.findAll();
        return MemberConverter.toTermsListDTO(termsList);
    }

    @Override
    public MemberResDTO.MemberProfileDTO findMemberProfile(Member member) {
        List<MemberCategory> memberCategories = memberCategoryRepository.findAllByMemberWithCategory(member);
        List<Contact> contacts = contactRepository.findAllByMember(member);

        return MemberConverter.toOwnerProfileDTO(member, memberCategories, contacts);
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

        return MemberConverter.toOtherProfileDTO(member, memberCategories, contacts);
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

        // 프로젝트 존재 및 소유자 확인
        Project project = projectRepository.findById(dto.projectId())
                .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.isOwnedBy(member)) {
            throw new ProjectException(ProjectErrorCode.FORBIDDEN_PROJECT_ACCESS);
        }

        // 벡터 검색 가능한 경우에만 추천 결과 반환
        if (isVectorSearchAvailable(dto.projectId())) {
            return executeVectorSearch(
                    dto.projectId(), member.getId(), dto.toPageable().getPageNumber(), dto.toPageable().getPageSize());
        }

        // 임베딩이 없으면 빈 배열 반환
        return PagedResponse.empty(dto.toPageable());
    }

    @Override
    public List<MemberResDTO.RecommendedDeveloperDTO> findRecommendedDevelopersPreview(Member member, Long projectId, int limit) {
        // projectId가 없으면 빈 배열 반환
        if (projectId == null) {
            return Collections.emptyList();
        }

        // 프로젝트 존재 및 소유자 확인
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.isOwnedBy(member)) {
            throw new ProjectException(ProjectErrorCode.FORBIDDEN_PROJECT_ACCESS);
        }

        // 벡터 검색 가능한 경우에만 추천 결과 반환
        if (isVectorSearchAvailable(projectId)) {
            return executeVectorSearchPreview(projectId, member.getId(), limit);
        }

        // 임베딩이 없으면 빈 배열 반환
        return Collections.emptyList();
    }

    /**
     * 벡터 검색 가능 여부 확인
     * embedding 컬럼을 직접 조회하지 않고 존재 여부만 확인
     */
    private boolean isVectorSearchAvailable(Long projectId) {
        try {
            return projectEmbeddingRepository.existsByProjectIdAndStatusAndEmbeddingIsNotNull(
                    projectId, EmbeddingStatus.SUCCESS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 벡터 검색을 실행하여 추천 개발자 목록 반환 (페이지네이션 포함)
     */
    private PagedResponse<MemberResDTO.RecommendedDeveloperDTO> executeVectorSearch(
            Long projectId, Long currentMemberId, int page, int size) {

        int offset = page * size;
        List<Object[]> results = memberRecommendRepository.findRecommendedDevelopers(projectId, currentMemberId, size, offset);

        if (results.isEmpty()) {
            long totalCount = memberRecommendRepository.countRecommendedDevelopers(projectId, currentMemberId);
            Page<MemberResDTO.RecommendedDeveloperDTO> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(page, size), totalCount);
            return PagedResponse.of(emptyPage, Collections.emptyList());
        }

        // 조회된 member ID 목록
        List<Long> memberIds = results.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        // Member 엔티티 조회
        List<Member> members = memberRepository.findAllById(memberIds);
        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        // N+1 방지: 카테고리, 기술스택 한 번에 조회
        List<MemberCategory> allMemberCategories = memberCategoryRepository.findAllByMemberInWithCategory(members);
        Map<Long, List<MemberCategory>> categoriesByMemberId = allMemberCategories.stream()
                .collect(Collectors.groupingBy(mc -> mc.getMember().getId()));

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(members);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        // 결과 변환 (순서 유지)
        List<MemberResDTO.RecommendedDeveloperDTO> developerDTOs = results.stream()
                .map(row -> {
                    Long memberId = ((Number) row[0]).longValue();
                    Double totalScore = row[4] != null ? ((Number) row[4]).doubleValue() : null;
                    Double similarityPercent = row[5] != null ? ((Number) row[5]).doubleValue() : null;
                    Double techstackPercent = row[6] != null ? ((Number) row[6]).doubleValue() : null;
                    Boolean domainMatch = row[7] != null ? (Boolean) row[7] : false;

                    Member developer = memberMap.get(memberId);
                    if (developer == null) {
                        return null;
                    }

                    List<MemberCategory> memberCategories = categoriesByMemberId
                            .getOrDefault(memberId, Collections.emptyList());
                    List<DevTechstack> devTechstacks = techstacksByMemberId
                            .getOrDefault(memberId, Collections.emptyList());

                    // 매칭된 기술스택 조회
                    List<String> matchedTechstacks = memberRecommendRepository
                            .findMatchedTechstacks(memberId, projectId);

                    return MemberConverter.toRecommendedDeveloperDTO(
                            developer, memberCategories, devTechstacks,
                            totalScore, similarityPercent, techstackPercent,
                            domainMatch, matchedTechstacks
                    );
                })
                .filter(dto -> dto != null)
                .toList();

        long totalCount = memberRecommendRepository.countRecommendedDevelopers(projectId, currentMemberId);
        Page<MemberResDTO.RecommendedDeveloperDTO> resultPage = new PageImpl<>(
                developerDTOs, PageRequest.of(page, size), totalCount);

        return PagedResponse.of(resultPage, developerDTOs);
    }

    /**
     * 벡터 검색을 실행하여 프리뷰용 추천 개발자 목록 반환
     */
    private List<MemberResDTO.RecommendedDeveloperDTO> executeVectorSearchPreview(Long projectId, Long currentMemberId, int limit) {
        List<Object[]> results = memberRecommendRepository.findRecommendedDevelopersPreview(projectId, currentMemberId, limit);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // 조회된 member ID 목록
        List<Long> memberIds = results.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        // Member 엔티티 조회
        List<Member> members = memberRepository.findAllById(memberIds);
        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        // N+1 방지: 카테고리, 기술스택 한 번에 조회
        List<MemberCategory> allMemberCategories = memberCategoryRepository.findAllByMemberInWithCategory(members);
        Map<Long, List<MemberCategory>> categoriesByMemberId = allMemberCategories.stream()
                .collect(Collectors.groupingBy(mc -> mc.getMember().getId()));

        List<DevTechstack> allDevTechstacks = devTechstackRepository.findAllByMemberInWithTechstack(members);
        Map<Long, List<DevTechstack>> techstacksByMemberId = allDevTechstacks.stream()
                .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

        // 결과 변환 (순서 유지)
        return results.stream()
                .map(row -> {
                    Long memberId = ((Number) row[0]).longValue();
                    Double totalScore = row[4] != null ? ((Number) row[4]).doubleValue() : null;
                    Double similarityPercent = row[5] != null ? ((Number) row[5]).doubleValue() : null;
                    Double techstackPercent = row[6] != null ? ((Number) row[6]).doubleValue() : null;
                    Boolean domainMatch = row[7] != null ? (Boolean) row[7] : false;

                    Member developer = memberMap.get(memberId);
                    if (developer == null) {
                        return null;
                    }

                    List<MemberCategory> memberCategories = categoriesByMemberId
                            .getOrDefault(memberId, Collections.emptyList());
                    List<DevTechstack> devTechstacks = techstacksByMemberId
                            .getOrDefault(memberId, Collections.emptyList());

                    // 매칭된 기술스택 조회
                    List<String> matchedTechstacks = memberRecommendRepository
                            .findMatchedTechstacks(memberId, projectId);

                    return MemberConverter.toRecommendedDeveloperDTO(
                            developer, memberCategories, devTechstacks,
                            totalScore, similarityPercent, techstackPercent,
                            domainMatch, matchedTechstacks
                    );
                })
                .filter(dto -> dto != null)
                .toList();
    }

    @Override
    public ProjectResDTO.MyProjectsRes findProjectsByNickname(String nickname, List<ProjectStatus> statuses, Pageable pageable) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        return projectQueryService.getMyProjects(member, statuses, pageable);
    }

    @Override
    public PagedResponse<MemberResDTO.GitRepoDTO> findReposByNickname(String nickname, Pageable pageable) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorCode.PROFILE_NOT_PUBLIC);
        }

        Page<GitRepoUrl> repoPage = gitRepoUrlRepository.findAllByMember(member, pageable);

        List<Long> gitRepoIds = repoPage.getContent().stream()
                .map(GitRepoUrl::getId)
                .collect(Collectors.toList());
        Set<Long> repoIdsWithReport = gitRepoIds.isEmpty()
                ? Set.of()
                : new HashSet<>(devReportRepository.findActiveReportGitRepoIds(gitRepoIds));

        List<MemberResDTO.GitRepoDTO> repoDTOs = repoPage.getContent().stream()
                .map(repo -> MemberConverter.toGitRepoDTO(repo, repoIdsWithReport.contains(repo.getId())))
                .collect(Collectors.toList());

        return PagedResponse.of(repoPage, repoDTOs);
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
