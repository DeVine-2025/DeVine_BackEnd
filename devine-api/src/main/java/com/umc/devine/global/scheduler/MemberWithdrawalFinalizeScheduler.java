package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberStatusHistory;
import com.umc.devine.domain.member.entity.WithdrawnMemberEmailHash;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.repository.WithdrawnMemberEmailHashRepository;
import com.umc.devine.domain.member.service.command.MemberGithubDataCleanupService;
import com.umc.devine.domain.member.util.EmailHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 강제탈퇴(자격상실) 30일 소명 절차가 만료된 계정을 최종 탈퇴 확정 처리한다.
 * MemberHardDeleteScheduler와 동일한 이유로 회원별 독립 트랜잭션(REQUIRES_NEW)에서 실행한다.
 * 특정 회원 처리 중 예외가 나도 그날 배치의 다른 회원들까지 함께 롤백되면 안 된다.
 */
@Slf4j
@Component
public class MemberWithdrawalFinalizeScheduler {

    private final MemberRepository memberRepository;
    private final MemberStatusHistoryRepository memberStatusHistoryRepository;
    private final ContactRepository contactRepository;
    private final MemberGithubDataCleanupService memberGithubDataCleanupService;
    private final WithdrawnMemberEmailHashRepository withdrawnMemberEmailHashRepository;
    private final EmailHasher emailHasher;
    private final TransactionTemplate transactionTemplate;

    public MemberWithdrawalFinalizeScheduler(
            MemberRepository memberRepository,
            MemberStatusHistoryRepository memberStatusHistoryRepository,
            ContactRepository contactRepository,
            MemberGithubDataCleanupService memberGithubDataCleanupService,
            WithdrawnMemberEmailHashRepository withdrawnMemberEmailHashRepository,
            EmailHasher emailHasher,
            PlatformTransactionManager transactionManager
    ) {
        this.memberRepository = memberRepository;
        this.memberStatusHistoryRepository = memberStatusHistoryRepository;
        this.contactRepository = contactRepository;
        this.memberGithubDataCleanupService = memberGithubDataCleanupService;
        this.withdrawnMemberEmailHashRepository = withdrawnMemberEmailHashRepository;
        this.emailHasher = emailHasher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void finalizeExpiredWithdrawals() {
        List<Long> expiredIds = memberRepository.findByUsedAndScheduledWithdrawalAtBefore(
                        MemberStatus.PENDING_WITHDRAWAL, LocalDateTime.now())
                .stream().map(Member::getId).toList();

        if (expiredIds.isEmpty()) {
            log.info("[MemberWithdrawalFinalize] 확정할 강제탈퇴 예정 계정 없음");
            return;
        }

        int finalized = 0;
        int failed = 0;
        for (Long memberId : expiredIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> finalizeOne(memberId));
                finalized++;
            } catch (RuntimeException e) {
                log.error("[MemberWithdrawalFinalize] 회원 {} 강제탈퇴 확정 실패, 다음 배치에서 재시도합니다.", memberId, e);
                failed++;
            }
        }
        log.info("[MemberWithdrawalFinalize] 완료 - 확정 {}건, 실패 {}건", finalized, failed);
    }

    private void finalizeOne(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        registerEmailHashToBlacklist(member, now);

        member.finalizeWithdrawal();
        memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(member)
                .action(MemberStatusAction.WITHDRAWAL_FINALIZED)
                .status(member.getUsed())
                .build());
        // 아래 벌크 삭제가 영속성 컨텍스트를 비우기 전에 위 변경분을 먼저 flush해둔다.
        memberRepository.saveAndFlush(member);

        memberGithubDataCleanupService.deleteGithubLinkedData(member);
    }

    /** 강제탈퇴 확정 시점의 이메일을 해시하여 1년간 재가입 제한 블랙리스트에 적재한다 (자진탈퇴는 대상이 아님). */
    private void registerEmailHashToBlacklist(Member member, LocalDateTime withdrawnAt) {
        contactRepository.findAllByMember(member).stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .map(Contact::getValue)
                .findFirst()
                .ifPresent(email -> withdrawnMemberEmailHashRepository.save(
                        WithdrawnMemberEmailHash.of(emailHasher.hash(email), withdrawnAt)));
    }
}
