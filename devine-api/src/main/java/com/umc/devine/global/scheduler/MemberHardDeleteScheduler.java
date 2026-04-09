package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴(soft delete)된 회원을 보관 기간 경과 후 hard delete 하는 배치.
 *
 * <p>FK 의존 도메인이 많아 잔여 참조가 있으면 삭제가 실패할 수 있으므로,
 * 기본은 비활성화이며 {@code member.hard-delete.enabled=true} 로 명시 활성화한다.
 * 실패한 행은 건너뛰고 로그만 남긴다 — 운영자가 사후 분석할 수 있도록.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "member.hard-delete.enabled", havingValue = "true")
public class MemberHardDeleteScheduler {

    private final MemberRepository memberRepository;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;

    @Value("${member.hard-delete.retention-days:30}")
    private int retentionDays;

    @Value("${member.hard-delete.chunk-size:100}")
    private int chunkSize;

    public MemberHardDeleteScheduler(MemberRepository memberRepository,
                                     PlatformTransactionManager transactionManager) {
        this.memberRepository = memberRepository;
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        log.info("[MemberHardDelete] 시작 - threshold={}, chunkSize={}", threshold, chunkSize);

        int totalDeleted = 0;
        int totalSkipped = 0;
        while (true) {
            List<Member> chunk = readTx.execute(status -> findChunk(threshold));
            if (chunk == null || chunk.isEmpty()) {
                break;
            }

            int deletedInChunk = 0;
            for (Member m : chunk) {
                Long id = m.getId();
                Boolean ok = writeTx.execute(status -> {
                    try {
                        memberRepository.deleteById(id);
                        memberRepository.flush();
                        return true;
                    } catch (DataIntegrityViolationException e) {
                        status.setRollbackOnly();
                        log.warn("[MemberHardDelete] FK 잔여 참조로 건너뜀 - memberId={}", id);
                        return false;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        log.warn("[MemberHardDelete] 삭제 중 예외 - memberId={}", id, e);
                        return false;
                    }
                });
                if (Boolean.TRUE.equals(ok)) {
                    deletedInChunk++;
                } else {
                    totalSkipped++;
                }
            }
            totalDeleted += deletedInChunk;

            // 청크가 가득 차지 않았다면 더 처리할 행이 없으므로 종료.
            // 청크가 모두 스킵됐다면 같은 행만 다시 잡혀 무한 루프가 되므로 종료.
            if (chunk.size() < chunkSize || deletedInChunk == 0) {
                break;
            }
        }

        log.info("[MemberHardDelete] 종료 - 삭제={}건, 스킵={}건", totalDeleted, totalSkipped);
    }

    private List<Member> findChunk(LocalDateTime threshold) {
        Pageable pageable = PageRequest.of(0, chunkSize);
        return memberRepository.findDeletedBefore(threshold, pageable);
    }
}
