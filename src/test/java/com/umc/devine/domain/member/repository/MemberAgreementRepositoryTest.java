package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberAgreement;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MemberAgreementRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private MemberAgreementRepository memberAgreementRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TermsRepository termsRepository;

    private Member testMember;
    private Terms requiredTerms;
    private Terms optionalTerms;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_test_123")
                .name("테스트")
                .nickname("testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        List<Terms> requiredTermsList = termsRepository.findAllByRequired(true);
        requiredTerms = requiredTermsList.get(0);

        optionalTerms = termsRepository.save(Terms.builder()
                .title("마케팅 수신 동의")
                .content("선택 약관 내용")
                .required(false)
                .build());
    }

    @Nested
    @DisplayName("findAllByMember")
    class FindAllByMemberTest {

        @Test
        @DisplayName("회원의 모든 약관 동의 내역을 조회한다")
        void findAllByMember_success() {
            // given
            memberAgreementRepository.save(MemberAgreement.builder()
                    .member(testMember)
                    .terms(requiredTerms)
                    .agreed(true)
                    .build());

            memberAgreementRepository.save(MemberAgreement.builder()
                    .member(testMember)
                    .terms(optionalTerms)
                    .agreed(false)
                    .build());

            // when
            List<MemberAgreement> result = memberAgreementRepository.findAllByMember(testMember);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("약관 동의 내역이 없으면 빈 리스트를 반환한다")
        void findAllByMember_empty() {
            // when
            List<MemberAgreement> result = memberAgreementRepository.findAllByMember(testMember);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMemberAndTerms")
    class FindByMemberAndTermsTest {

        @Test
        @DisplayName("회원과 약관으로 동의 내역을 조회한다")
        void findByMemberAndTerms_success() {
            // given
            MemberAgreement agreement = memberAgreementRepository.save(MemberAgreement.builder()
                    .member(testMember)
                    .terms(requiredTerms)
                    .agreed(true)
                    .build());

            // when
            Optional<MemberAgreement> result = memberAgreementRepository.findByMemberAndTerms(testMember, requiredTerms);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getAgreed()).isTrue();
        }

        @Test
        @DisplayName("동의 내역이 없으면 빈 Optional을 반환한다")
        void findByMemberAndTerms_notFound() {
            // when
            Optional<MemberAgreement> result = memberAgreementRepository.findByMemberAndTerms(testMember, requiredTerms);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByMemberAndTerms")
    class ExistsByMemberAndTermsTest {

        @Test
        @DisplayName("동의 내역이 존재하면 true를 반환한다")
        void existsByMemberAndTerms_true() {
            // given
            memberAgreementRepository.save(MemberAgreement.builder()
                    .member(testMember)
                    .terms(requiredTerms)
                    .agreed(true)
                    .build());

            // when
            boolean result = memberAgreementRepository.existsByMemberAndTerms(testMember, requiredTerms);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("동의 내역이 없으면 false를 반환한다")
        void existsByMemberAndTerms_false() {
            // when
            boolean result = memberAgreementRepository.existsByMemberAndTerms(testMember, requiredTerms);

            // then
            assertThat(result).isFalse();
        }
    }
}
