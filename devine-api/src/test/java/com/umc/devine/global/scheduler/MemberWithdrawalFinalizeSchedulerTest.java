package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.repository.WithdrawnMemberEmailHashRepository;
import com.umc.devine.domain.member.util.EmailHasher;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 스케줄러가 회원별 독립 트랜잭션(REQUIRES_NEW)으로 동작해 테스트의 기본 롤백 방식으론 검증이 안 되므로, 트랜잭션 래핑을 끄고 직접 정리한다. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberWithdrawalFinalizeSchedulerTest extends IntegrationTestSupport {

    @Autowired
    private MemberWithdrawalFinalizeScheduler scheduler;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private MemberStatusHistoryRepository memberStatusHistoryRepository;

    @Autowired
    private WithdrawnMemberEmailHashRepository withdrawnMemberEmailHashRepository;

    @Autowired
    private EmailHasher emailHasher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private Member expiredMember;
    private Member notYetDueMember;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            expiredMember = memberRepository.save(Member.builder()
                    .clerkId("clerk_expired")
                    .name("만료유저")
                    .nickname("expireduser")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());
            expiredMember.scheduleForceWithdrawal(LocalDateTime.now().minusDays(1));
            memberRepository.save(expiredMember);

            notYetDueMember = memberRepository.save(Member.builder()
                    .clerkId("clerk_pending")
                    .name("대기유저")
                    .nickname("pendinguser")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());
            notYetDueMember.scheduleForceWithdrawal(LocalDateTime.now().plusDays(10));
            memberRepository.save(notYetDueMember);
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            memberRepository.findById(expiredMember.getId()).ifPresent(m -> {
                memberStatusHistoryRepository.bulkDeleteByMember(m);
                contactRepository.deleteAllByMember(m);
                memberRepository.delete(m);
            });
            memberRepository.findById(notYetDueMember.getId()).ifPresent(m -> {
                memberStatusHistoryRepository.bulkDeleteByMember(m);
                contactRepository.deleteAllByMember(m);
                memberRepository.delete(m);
            });
        });
    }

    @Test
    @DisplayName("예정일시가 지난 계정만 DELETED로 최종 확정되고 PII가 익명화된다")
    void finalizeExpiredWithdrawals_finalizesOnlyExpired() {
        // when
        scheduler.finalizeExpiredWithdrawals();

        // then
        Member expired = memberRepository.findById(expiredMember.getId()).orElseThrow();
        Member notYetDue = memberRepository.findById(notYetDueMember.getId()).orElseThrow();

        assertThat(expired.getUsed()).isEqualTo(MemberStatus.DELETED);
        assertThat(expired.getScheduledWithdrawalAt()).isNull();
        assertThat(expired.getName()).isNull();
        assertThat(expired.getNickname()).isNotEqualTo("expireduser");
        assertThat(notYetDue.getUsed()).isEqualTo(MemberStatus.PENDING_WITHDRAWAL);
        assertThat(notYetDue.getName()).isEqualTo("대기유저");
    }

    @Test
    @DisplayName("강제탈퇴 확정 시 연락처 등 GitHub 연동 데이터가 즉시 삭제된다")
    void finalizeExpiredWithdrawals_deletesGithubLinkedData() {
        // given
        transactionTemplate.executeWithoutResult(status -> contactRepository.save(Contact.builder()
                .contactType(ContactType.EMAIL)
                .value("expired-linked@example.com")
                .member(expiredMember)
                .build()));

        // when
        scheduler.finalizeExpiredWithdrawals();

        // then
        Member expired = memberRepository.findById(expiredMember.getId()).orElseThrow();
        assertThat(contactRepository.findAllByMember(expired)).isEmpty();
    }

    @Test
    @DisplayName("만료된 회원이 여러 명이면 각자 독립된 트랜잭션에서 확정 처리된다")
    void finalizeExpiredWithdrawals_processesMultipleExpiredMembersInOneRun() {
        // given: 만료된 회원을 하나 더 추가 (기존 expiredMember와 함께 총 2명)
        Member secondExpiredMember = transactionTemplate.execute(status -> {
            Member m = memberRepository.save(Member.builder()
                    .clerkId("clerk_expired_2")
                    .name("만료유저2")
                    .nickname("expireduser2")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());
            m.scheduleForceWithdrawal(LocalDateTime.now().minusDays(1));
            memberRepository.save(m);

            contactRepository.save(Contact.builder()
                    .contactType(ContactType.EMAIL)
                    .value("expired1@example.com")
                    .member(expiredMember)
                    .build());
            contactRepository.save(Contact.builder()
                    .contactType(ContactType.EMAIL)
                    .value("expired2@example.com")
                    .member(m)
                    .build());
            return m;
        });

        // when — 한 회원의 처리가 다른 회원의 확정 처리에 영향을 주지 않는지 검증
        scheduler.finalizeExpiredWithdrawals();

        // then
        Member first = memberRepository.findById(expiredMember.getId()).orElseThrow();
        Member second = memberRepository.findById(secondExpiredMember.getId()).orElseThrow();

        assertThat(first.getUsed()).isEqualTo(MemberStatus.DELETED);
        assertThat(first.getName()).isNull();
        assertThat(second.getUsed()).isEqualTo(MemberStatus.DELETED);
        assertThat(second.getName()).isNull();
        assertThat(second.getNickname()).isNotEqualTo("expireduser2");

        assertThat(contactRepository.findAllByMember(first)).isEmpty();
        assertThat(contactRepository.findAllByMember(second)).isEmpty();

        assertThat(withdrawnMemberEmailHashRepository.existsActiveByEmailHash(
                emailHasher.hash("expired1@example.com"), LocalDateTime.now())).isTrue();
        assertThat(withdrawnMemberEmailHashRepository.existsActiveByEmailHash(
                emailHasher.hash("expired2@example.com"), LocalDateTime.now())).isTrue();

        // cleanup
        transactionTemplate.executeWithoutResult(status -> {
            memberStatusHistoryRepository.bulkDeleteByMember(second);
            memberRepository.delete(second);
        });
    }

    @Test
    @DisplayName("강제탈퇴 확정 시 이메일 해시가 블랙리스트에 적재된다")
    void finalizeExpiredWithdrawals_registersEmailHashToBlacklist() {
        // given
        transactionTemplate.executeWithoutResult(status -> contactRepository.save(Contact.builder()
                .contactType(ContactType.EMAIL)
                .value("expired@example.com")
                .member(expiredMember)
                .build()));

        // when
        scheduler.finalizeExpiredWithdrawals();

        // then
        String emailHash = emailHasher.hash("expired@example.com");
        assertThat(withdrawnMemberEmailHashRepository.existsActiveByEmailHash(emailHash, LocalDateTime.now())).isTrue();
    }
}
