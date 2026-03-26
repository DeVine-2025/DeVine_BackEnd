package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContactRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

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
    }

    @Nested
    @DisplayName("findAllByMember")
    class FindAllByMemberTest {

        @Test
        @DisplayName("회원의 모든 연락처를 조회한다")
        void findAllByMember_success() {
            // given
            Contact contact1 = contactRepository.save(Contact.builder()
                    .member(testMember)
                    .contactType(ContactType.EMAIL)
                    .value("test@example.com")
                    .build());

            Contact contact2 = contactRepository.save(Contact.builder()
                    .member(testMember)
                    .contactType(ContactType.GITHUB)
                    .value("https://github.com/testuser")
                    .build());

            // when
            List<Contact> result = contactRepository.findAllByMember(testMember);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Contact::getContactType)
                    .containsExactlyInAnyOrder(ContactType.EMAIL, ContactType.GITHUB);
        }

        @Test
        @DisplayName("연락처가 없으면 빈 리스트를 반환한다")
        void findAllByMember_empty() {
            // when
            List<Contact> result = contactRepository.findAllByMember(testMember);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteAllByMember")
    class DeleteAllByMemberTest {

        @Test
        @DisplayName("회원의 모든 연락처를 삭제한다")
        void deleteAllByMember_success() {
            // given
            contactRepository.save(Contact.builder()
                    .member(testMember)
                    .contactType(ContactType.EMAIL)
                    .value("test@example.com")
                    .build());

            contactRepository.save(Contact.builder()
                    .member(testMember)
                    .contactType(ContactType.GITHUB)
                    .value("https://github.com/testuser")
                    .build());

            // when
            contactRepository.deleteAllByMember(testMember);

            // then
            List<Contact> remaining = contactRepository.findAllByMember(testMember);
            assertThat(remaining).isEmpty();
        }
    }
}
