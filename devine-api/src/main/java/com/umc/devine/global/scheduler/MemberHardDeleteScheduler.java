package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.scheduler.harddelete.MemberHardDeleteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 유예기간이 지난 계정의 개인정보 관련 데이터를 완전 삭제한다. 실제 정리 작업은
 * {@link MemberHardDeleteHandler} 구현체들(Spring이 {@link org.springframework.core.annotation.Order}
 * 순서대로 주입)이 각자 담당하고, 이 클래스는 대상 조회, 회원별 트랜잭션 격리, 최종 회원 행 삭제만 책임진다.
 * 새 회원 연관 테이블이 생기면 핸들러를 추가하면 되고, 이 클래스는 수정할 필요가 없다.
 *
 * payment/matching/project/chat 등 다른 회원과 얽힌 레코드가 남아있어 FK 위반이 나면 해당 회원만
 * 건너뛰고 다음 배치에서 재시도한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "member.hard-delete.enabled", havingValue = "true")
public class MemberHardDeleteScheduler {

    private final MemberRepository memberRepository;
    private final List<MemberHardDeleteHandler> handlers;
    private final TransactionTemplate transactionTemplate;

    @Value("${member.hard-delete.grace-period-days:30}")
    private int gracePeriodDays;

    public MemberHardDeleteScheduler(
            MemberRepository memberRepository,
            List<MemberHardDeleteHandler> handlers,
            PlatformTransactionManager transactionManager
    ) {
        this.memberRepository = memberRepository;
        this.handlers = handlers;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // 회원별로 완전히 독립된 물리 트랜잭션에서 실행되어야, 한 회원의 FK 위반이 다른 회원의 삭제를 막지 않는다.
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void hardDeleteExpiredWithdrawals() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(gracePeriodDays);
        List<Long> candidateIds = memberRepository.findByUsedAndDeletedAtBefore(MemberStatus.DELETED, threshold)
                .stream().map(Member::getId).toList();

        if (candidateIds.isEmpty()) {
            log.info("[MemberHardDelete] 삭제 대상 회원 없음");
            return;
        }

        int deleted = 0;
        int skipped = 0;
        for (Long memberId : candidateIds) {
            Boolean success = transactionTemplate.execute(status -> {
                try {
                    hardDeleteOne(memberId);
                    return true;
                } catch (DataIntegrityViolationException e) {
                    log.warn("[MemberHardDelete] 회원 {} - 비즈니스 레코드가 남아있어 하드삭제를 건너뜁니다.", memberId);
                    status.setRollbackOnly();
                    return false;
                }
            });
            if (Boolean.TRUE.equals(success)) {
                deleted++;
            } else {
                skipped++;
            }
        }
        log.info("[MemberHardDelete] 완료 - 삭제 {}건, 건너뜀 {}건", deleted, skipped);
    }

    private void hardDeleteOne(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        handlers.forEach(handler -> handler.handle(member));

        // payment/matching/project/chat 등이 여전히 참조 중이면 FK 위반으로 실패하고 호출부에서 건너뛴다.
        memberRepository.delete(member);
        memberRepository.flush();
    }
}
