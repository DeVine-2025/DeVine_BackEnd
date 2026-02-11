package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.domain.techstack.exception.TechstackException;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberCategoryRepository memberCategoryRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private TechstackRepository techstackRepository;

    @Autowired
    private DevTechstackRepository devTechstackRepository;

    @Autowired
    private ImageRepository imageRepository;

    private Member testMember;
    private Category testCategory;
    private Techstack testTechstack;
    private Terms requiredTerms;

    @BeforeEach
    void setUp() {
        requiredTerms = termsRepository.save(Terms.builder()
                .title("서비스 이용약관")
                .content("약관 내용")
                .required(true)
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.HEALTHCARE)
                .build());

        testTechstack = techstackRepository.save(Techstack.builder()
                .name(TechName.JAVA)
                .genre(TechGenre.LANGUAGE)
                .build());

        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_test_123")
                .name("테스트")
                .nickname("testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    @Nested
    @DisplayName("회원가입")
    class SignupTest {

        @Test
        @DisplayName("회원가입 성공")
        void signup_success() {
            // given
            ClerkPrincipal principal = new ClerkPrincipal(
                    "clerk_new_user",
                    "newuser@example.com",
                    "새사용자",
                    null
            );

            MemberReqDTO.SignupDTO dto = MemberReqDTO.SignupDTO.builder()
                    .agreements(List.of(
                            MemberReqDTO.AgreementDTO.builder()
                                    .termsId(requiredTerms.getId())
                                    .agreed(true)
                                    .build()
                    ))
                    .nickname("newuser")
                    .mainType(MemberMainType.DEVELOPER)
                    .categoryIds(List.of(testCategory.getId()))
                    .build();

            // when
            MemberResDTO.SignupResultDTO result = memberCommandService.signup(principal, dto);

            // then
            assertThat(result.nickname()).isEqualTo("newuser");
            assertThat(result.mainType()).isEqualTo(MemberMainType.DEVELOPER);

            Member savedMember = memberRepository.findByNickname("newuser").orElseThrow();
            assertThat(savedMember.getClerkId()).isEqualTo("clerk_new_user");
        }

        @Test
        @DisplayName("존재하지 않는 프로필 이미지 URL 사용 시 예외 발생")
        void signup_imageNotFound() {
            // given
            ClerkPrincipal principal = new ClerkPrincipal("clerk_img_fail", "img@example.com", "이미지실패", null);
            MemberReqDTO.SignupDTO dto = MemberReqDTO.SignupDTO.builder()
                    .agreements(List.of(MemberReqDTO.AgreementDTO.builder().termsId(requiredTerms.getId()).agreed(true).build()))
                    .nickname("imgfail")
                    .mainType(MemberMainType.DEVELOPER)
                    .categoryIds(List.of(testCategory.getId()))
                    .imageUrl("https://invalid-image-url.com/profile.jpg")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberCommandService.signup(principal, dto))
                    .isInstanceOf(ImageException.class);
        }

        @Test
        @DisplayName("필수 약관이 여러 개일 때 하나라도 미동의 시 예외 발생")
        void signup_multipleRequiredTerms() {
            // given
            Terms requiredTerms2 = termsRepository.save(Terms.builder()
                    .title("개인정보 처리방침")
                    .content("내용")
                    .required(true)
                    .build());

            ClerkPrincipal principal = new ClerkPrincipal("clerk_terms_fail", "terms@example.com", "약관실패", null);
            MemberReqDTO.SignupDTO dto = MemberReqDTO.SignupDTO.builder()
                    .agreements(List.of(
                            MemberReqDTO.AgreementDTO.builder().termsId(requiredTerms.getId()).agreed(true).build(),
                            MemberReqDTO.AgreementDTO.builder().termsId(requiredTerms2.getId()).agreed(false).build() // 하나 미동의
                    ))
                    .nickname("termsfail")
                    .mainType(MemberMainType.DEVELOPER)
                    .categoryIds(List.of(testCategory.getId()))
                    .build();

            // when & then
            assertThatThrownBy(() -> memberCommandService.signup(principal, dto))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("기 가입 회원의 경우 예외 발생")
        void signup_alreadyRegistered() {
            // given
            ClerkPrincipal principal = new ClerkPrincipal(
                    "clerk_test_123",
                    "test@example.com",
                    "테스트",
                    null
            );

            MemberReqDTO.SignupDTO dto = MemberReqDTO.SignupDTO.builder()
                    .agreements(List.of(
                            MemberReqDTO.AgreementDTO.builder()
                                    .termsId(requiredTerms.getId())
                                    .agreed(true)
                                    .build()
                    ))
                    .nickname("duplicateuser")
                    .mainType(MemberMainType.DEVELOPER)
                    .categoryIds(List.of(testCategory.getId()))
                    .build();

            // when & then
            assertThatThrownBy(() -> memberCommandService.signup(principal, dto))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("필수 약관 미동의 시 예외 발생")
        void signup_requiredTermsNotAgreed() {
            // given
            ClerkPrincipal principal = new ClerkPrincipal(
                    "clerk_new_user_2",
                    "newuser2@example.com",
                    "새사용자2",
                    null
            );

            MemberReqDTO.SignupDTO dto = MemberReqDTO.SignupDTO.builder()
                    .agreements(List.of(
                            MemberReqDTO.AgreementDTO.builder()
                                    .termsId(requiredTerms.getId())
                                    .agreed(false)
                                    .build()
                    ))
                    .nickname("newuser2")
                    .mainType(MemberMainType.DEVELOPER)
                    .categoryIds(List.of(testCategory.getId()))
                    .build();

            // when & then
            assertThatThrownBy(() -> memberCommandService.signup(principal, dto))
                    .isInstanceOf(MemberException.class);
        }
    }

    @Nested
    @DisplayName("회원 정보 수정")
    class UpdateMemberTest {

        @Test
        @DisplayName("회원 정보 수정 성공")
        void updateMember_success() {
            // given
            MemberReqDTO.UpdateMemberDTO dto = MemberReqDTO.UpdateMemberDTO.builder()
                    .nickname("updateduser")
                    .body("자기소개 수정")
                    .address("서울시 강남구")
                    .build();

            // when
            MemberResDTO.MemberProfileDTO result = memberCommandService.updateMember(testMember, dto);

            // then
            assertThat(result.member().nickname()).isEqualTo("updateduser");
            assertThat(result.member().body()).isEqualTo("자기소개 수정");
            assertThat(result.member().address()).isEqualTo("서울시 강남구");
        }

        @Test
        @DisplayName("중복 닉네임으로 수정 시 예외 발생")
        void updateMember_duplicateNickname() {
            // given
            Member anotherMember = memberRepository.save(Member.builder()
                    .clerkId("clerk_another_123")
                    .name("다른사용자")
                    .nickname("anotheruser")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            MemberReqDTO.UpdateMemberDTO dto = MemberReqDTO.UpdateMemberDTO.builder()
                    .nickname("anotheruser")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberCommandService.updateMember(testMember, dto))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("기존 닉네임 유지 시 성공")
        void updateMember_sameNickname() {
            // given
            MemberReqDTO.UpdateMemberDTO dto = MemberReqDTO.UpdateMemberDTO.builder()
                    .nickname("testuser")
                    .body("자기소개만 수정")
                    .build();

            // when
            MemberResDTO.MemberProfileDTO result = memberCommandService.updateMember(testMember, dto);

            // then
            assertThat(result.member().nickname()).isEqualTo("testuser");
            assertThat(result.member().body()).isEqualTo("자기소개만 수정");
        }
    }

    @Nested
    @DisplayName("기술 스택 추가")
    class AddMemberTechstacksTest {

        @Test
        @DisplayName("기술 스택 추가 성공")
        void addMemberTechstacks_success() {
            // given
            Techstack newTechstack = techstackRepository.save(Techstack.builder()
                    .name(TechName.SPRINGBOOT)
                    .genre(TechGenre.FRAMEWORK)
                    .build());

            MemberReqDTO.AddTechstackDTO dto = MemberReqDTO.AddTechstackDTO.builder()
                    .techstackIds(new Long[] {newTechstack.getId()})
                    .build();

            // when
            TechstackResDTO.DevTechstackListDTO result = memberCommandService.addMemberTechstacks(testMember, dto);

            // then
            assertThat(result.techstacks()).hasSize(1);
        }

        @Test
        @DisplayName("중복 기술 스택 추가 시 예외 발생")
        void addMemberTechstacks_alreadyExists() {
            // given
            DevTechstack existing = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(existing);

            MemberReqDTO.AddTechstackDTO dto = MemberReqDTO.AddTechstackDTO.builder()
                    .techstackIds(new Long[] {testTechstack.getId()})
                    .build();

            // when & then
            assertThatThrownBy(() -> memberCommandService.addMemberTechstacks(testMember, dto))
                    .isInstanceOf(MemberException.class);
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class WithdrawTest {

        @Test
        @DisplayName("회원 탈퇴 시 상태가 DELETED로 변경된다")
        void withdraw_success() {
            // when
            memberCommandService.withdraw(testMember);

            // then
            assertThat(testMember.getUsed()).isEqualTo(MemberStatus.DELETED);
            
            // 리포지토리 조회 시에도 반영되었는지 확인
            boolean exists = memberRepository.existsByNickname("testuser");
            assertThat(exists).isFalse(); // 리포지토리 쿼리에 used='ACTIVE' 조건이 있으므로 false여야 함
        }
    }

    @Nested
    @DisplayName("기술 스택 삭제 상세 검증")
    class RemoveTechstackDetailTest {

        @Test
        @DisplayName("존재하지 않는 기술 스택 ID를 삭제하려 하면 예외가 발생한다")
        void removeMemberTechstacks_notFound() {
            // given
            MemberReqDTO.RemoveTechstackDTO dto = MemberReqDTO.RemoveTechstackDTO.builder()
                    .techstackIds(new Long[] {999L}) // 존재하지 않는 ID
                    .build();

            // when & then
            assertThatThrownBy(() -> memberCommandService.removeMemberTechstacks(testMember, dto))
                    .isInstanceOf(TechstackException.class);
        }
    }
}
