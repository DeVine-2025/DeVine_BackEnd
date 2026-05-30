package com.umc.devine.domain.member.event;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MemberSignedUpEventListener 통합 테스트.
 * AFTER_COMMIT 리스너가 트랜잭션 커밋 후 실행되는지 검증한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberSignedUpEventListenerTest extends IntegrationTestSupport {

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberReportCreditRepository memberReportCreditRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.saveAndFlush(Member.builder()
                .clerkId("clerk_listener_test")
                .name("리스너테스트")
                .nickname("listener_testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        memberReportCreditRepository.findByMember(testMember)
                .ifPresent(memberReportCreditRepository::delete);
        memberRepository.deleteById(testMember.getId());
    }

    @Test
    @DisplayName("MemberSignedUpEvent 발행 후 트랜잭션 커밋 시 초기 크레딧 행이 생성된다")
    void memberSignedUpEvent_afterCommit_initializesCredit() {
        // given - 트랜잭션 내에서 이벤트 발행 (AFTER_COMMIT 리스너는 커밋 후 실행됨)
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new MemberSignedUpEvent(testMember))
        );

        // then - 커밋 후 리스너가 실행되어 크레딧 행이 생성되어야 한다
        assertThat(memberReportCreditRepository.findByMember(testMember)).isPresent();
    }
}
