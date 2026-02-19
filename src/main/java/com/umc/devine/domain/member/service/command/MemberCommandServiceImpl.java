package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.category.converter.CategoryConverter;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.exception.code.ImageErrorCode;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.converter.MemberConverter;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberAgreement;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberAgreementRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.techstack.converter.TechstackConverter;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.exception.TechstackException;
import com.umc.devine.domain.techstack.exception.code.TechstackErrorCode;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.infrastructure.github.GitHubService;
import com.umc.devine.infrastructure.github.dto.GitHubRepositoryDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ContactRepository contactRepository;
    private final TechstackRepository techstackRepository;
    private final DevTechstackRepository devTechstackRepository;
    private final ImageRepository imageRepository;
    private final TermsRepository termsRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final GitHubService gitHubService;
    private final DevReportRepository devReportRepository;
    private final EntityManager entityManager;

    @Override
    public MemberResDTO.SignupResultDTO signup(ClerkPrincipal principal, MemberReqDTO.SignupDTO dto) {
        // 1. 이미 가입된 회원인지 확인
        if (memberRepository.existsByClerkId(principal.getClerkId())) {
            throw new MemberException(MemberErrorCode.ALREADY_REGISTERED);
        }

        // 2. 프로필 이미지 검증 (선택사항)
        validateProfileImage(dto.imageUrl());

        // 3. 약관 검증 및 필수 약관 동의 확인
        List<Terms> allTerms = termsRepository.findAll();
        List<Terms> requiredTerms = allTerms.stream()
                .filter(Terms::getRequired)
                .toList();

        for (Terms required : requiredTerms) {
            boolean agreed = dto.agreements().stream()
                    .filter(a -> a.termsId().equals(required.getId()))
                    .findFirst()
                    .map(MemberReqDTO.AgreementDTO::agreed)
                    .orElse(false);

            if (!agreed) {
                throw new MemberException(MemberErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }
        }

        // 4. 관심 도메인 검증
        List<Category> categories = categoryRepository.findAllById(dto.categoryIds());
        if (categories.size() != dto.categoryIds().size()) {
            throw new MemberException(MemberErrorCode.CATEGORY_NOT_FOUND);
        }

        // 5. 기술 스택 검증 (선택사항)
        List<Techstack> techstacks = new ArrayList<>();
        if (dto.techstackIds() != null && !dto.techstackIds().isEmpty()) {
            techstacks = techstackRepository.findAllById(dto.techstackIds());
            if (techstacks.size() != dto.techstackIds().size()) {
                throw new MemberException(MemberErrorCode.TECHSTACK_NOT_FOUND);
            }
        }

        // 6. Member 생성 및 저장
        Member member = MemberConverter.toMember(dto, principal);
        memberRepository.save(member);

        // 7. 약관 동의 저장
        List<MemberAgreement> agreements = MemberConverter.toMemberAgreements(member, allTerms, dto.agreements());
        memberAgreementRepository.saveAll(agreements);

        // 8. 관심 도메인 저장
        List<MemberCategory> memberCategories = MemberConverter.toMemberCategories(member, categories);
        memberCategoryRepository.saveAll(memberCategories);

        // 9. 기술 스택 저장 (선택사항)
        if (!techstacks.isEmpty()) {
            List<DevTechstack> devTechstacks = MemberConverter.toDevTechstacks(member, techstacks);
            devTechstackRepository.saveAll(devTechstacks);
        }

        // 10. 연락처 저장 (선택사항)
        List<Contact> contacts = new ArrayList<>();
        if (principal.getEmail() != null) {
            contacts.add(MemberConverter.toSignupContact(member, ContactType.EMAIL, principal.getEmail()));
        }
        if (dto.linkedin() != null && !dto.linkedin().isBlank()) {
            contacts.add(MemberConverter.toSignupContact(member, ContactType.LINKEDIN, dto.linkedin()));
        }
        if (!contacts.isEmpty()) {
            contactRepository.saveAll(contacts);
        }

        return MemberConverter.toSignupResultDTO(member);
    }

    @Override
    public MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto) {

        // 닉네임 중복 검증
        if (dto.nickname() != null && !dto.nickname().equals(member.getNickname())) {
            if (memberRepository.existsByNickname(dto.nickname())) {
                throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATED);
            }
        }

        // 프로필 이미지 검증
        validateProfileImage(dto.imageUrl());

        // 멤버 업데이트
        member.updateProfile(dto);

        // 카테고리 업데이트 (orphanRemoval + flush로 DELETE 후 INSERT 보장)
        if (dto.domains() != null) {
            if (dto.domains().length == 0) {
                throw new MemberException(MemberErrorCode.CATEGORY_REQUIRED);
            }
            List<Category> categories = categoryRepository.findAllByGenreIn(Arrays.asList(dto.domains()));
            if (categories.size() != dto.domains().length) {
                throw new MemberException(MemberErrorCode.CATEGORY_NOT_FOUND);
            }
            member.clearCategories();
            entityManager.flush();  // DELETE 먼저 실행
            member.addCategories(categories);
        }

        // 연락처 업데이트
        if (dto.contacts() != null && dto.contacts().length > 0) {
            contactRepository.deleteAllByMember(member);
            contactRepository.saveAll(MemberConverter.toContacts(member, dto.contacts()));
        }

        return MemberConverter.toMemberProfileDTO(
                member,
                memberCategoryRepository.findAllByMemberWithCategory(member),
                contactRepository.findAllByMember(member)
        );
    }

    @Override
    public TechstackResDTO.DevTechstackListDTO addMemberTechstacks(Member member, MemberReqDTO.AddTechstackDTO dto) {
        List<Techstack> techstacks = techstackRepository.findAllById(Arrays.asList(dto.techstackIds()));

        if (techstacks.size() != dto.techstackIds().length) {
            throw new TechstackException(TechstackErrorCode.NOT_FOUND);
        }

        List<DevTechstack> existingTechstacks = devTechstackRepository.findAllByMemberAndTechstackIn(member, techstacks);
        if (!existingTechstacks.isEmpty()) {
            throw new MemberException(MemberErrorCode.TECHSTACK_ALREADY_EXISTS);
        }

        devTechstackRepository.saveAll(
                TechstackConverter.toDevTechstacks(member, techstacks, TechstackSource.MANUAL)
        );

        return TechstackConverter.toDevTechstackListDTO(devTechstackRepository.findAllByMemberWithTechstack(member));
    }

    @Override
    public TechstackResDTO.DevTechstackListDTO removeMemberTechstacks(Member member, MemberReqDTO.RemoveTechstackDTO dto) {
        List<Techstack> techstacks = techstackRepository.findAllById(Arrays.asList(dto.techstackIds()));

        if (techstacks.size() != dto.techstackIds().length) {
            throw new TechstackException(TechstackErrorCode.NOT_FOUND);
        }

        List<DevTechstack> targetTechstacks = devTechstackRepository.findAllByMemberAndTechstackInWithTechstack(member, techstacks);

        List<DevTechstack> deletedTechstacks = targetTechstacks.stream()
                .filter(dt -> dto.source() == null || dt.getSource() == dto.source())
                .toList();

        devTechstackRepository.deleteAll(deletedTechstacks);

        return TechstackConverter.toDevTechstackListDTO(deletedTechstacks);
    }

    // 탈퇴 기능을 위한 장치
    @Override
    public void withdraw(Member member) {
        // 테스트 코드 등에서 회원의 상태를 DELETED로 변경하여 조회 필터링을 검증하기 위해 사용됩니다.
        member.withdraw();
        memberRepository.save(member);
    }

    @Override
    public PagedResponse<MemberResDTO.GitRepoDTO> syncGitHubRepositories(Member member, MemberReqDTO.GitRepoSyncDTO dto) {
        // GitHub username이 없으면 저장
        if (member.getGithubUsername() == null) {
            Map<String, Object> userInfo = gitHubService.getUserInfo(member.getClerkId());
            String githubUsername = (String) userInfo.get("login");
            member.updateGithubUsername(githubUsername);
        }

        List<GitHubRepositoryDTO> githubRepos = gitHubService.getContributedRepositories(member.getClerkId());

        // 기존 레포를 한 번에 조회하여 Map으로 변환 (gitUrl -> GitRepoUrl)
        Map<String, GitRepoUrl> existingRepos = gitRepoUrlRepository.findAllByMember(member).stream()
                .collect(Collectors.toMap(GitRepoUrl::getGitUrl, Function.identity()));

        List<GitRepoUrl> newRepos = new ArrayList<>();

        for (GitHubRepositoryDTO repo : githubRepos) {
            GitRepoUrl existing = existingRepos.get(repo.getHtmlUrl());

            if (existing != null) {
                // 값이 같으면 dirty 표시 안됨 → UPDATE 쿼리 발생 안함
                existing.updateDescription(repo.getDescription());
            } else {
                GitRepoUrl newRepo = GitRepoUrl.builder()
                        .member(member)
                        .gitUrl(repo.getHtmlUrl())
                        .gitDescription(repo.getDescription())
                        .build();
                newRepos.add(newRepo);
            }
        }

        if (!newRepos.isEmpty()) {
            gitRepoUrlRepository.saveAll(newRepos);
        }

        // 페이지네이션된 결과 조회
        Page<GitRepoUrl> repoPage = gitRepoUrlRepository.findAllByMemberOrderByCreatedAtDesc(member, dto.toPageable());

        // 리포트 존재 여부 배치 조회
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

    private void validateProfileImage(String imageUrl) {
        if (imageUrl == null) return;

        Image image = imageRepository.findByImageUrl(imageUrl)
                .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_NOT_FOUND));
        if (image.getImageType() != ImageType.PROFILE) {
            throw new ImageException(ImageErrorCode.IMAGE_TYPE_MISMATCH);
        }
        if (!image.isUploaded()) {
            throw new ImageException(ImageErrorCode.IMAGE_NOT_UPLOADED);
        }
    }
}