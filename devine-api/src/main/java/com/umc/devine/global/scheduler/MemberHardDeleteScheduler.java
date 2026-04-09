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
 * <p>기본은 비활성화이며 {@code member.hard-delete.enabled=true} 로 명시 활성화한다.
 *
 * <p>각 회원마다 별도 트랜잭션으로 FK 종속 테이블을 정리한 뒤 member 행을 삭제한다.
 * payment, matching, project, chat 등 비즈니스 레코드가 남아 있으면
 * FK 위반으로 skip 하고 로그를 남긴다.
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
            List<Long> ids = readTx.execute(status -> {
                Pageable pageable = PageRequest.of(0, chunkSize);
                return memberRepository.findDeletedBefore(threshold, pageable)
                        .stream().map(Member::getId).toList();
            });
            if (ids == null || ids.isEmpty()) {
                break;
            }

            int deletedInChunk = 0;
            for (Long id : ids) {
                Boolean ok = writeTx.execute(status -> {
                    try {
                        // FK 체인 역순: report_embedding → dev_report → git_repo_url
                        memberRepository.hardDeleteReportEmbeddingsOf(id);
                        memberRepository.hardDeleteDevReportsOf(id);
                        memberRepository.hardDeleteGitRepoUrlsOf(id);

                        memberRepository.hardDeleteContactsOf(id);
                        memberRepository.hardDeleteDevTechstacksOf(id);
                        memberRepository.hardDeleteMemberCategoriesOf(id);
                        memberRepository.hardDeleteMemberAgreementsOf(id);
                        memberRepository.hardDeleteBookmarksOf(id);
                        memberRepository.hardDeleteMemberReportCreditsOf(id);
                        memberRepository.hardDeleteImagesOf(id);
                        memberRepository.hardDeleteNotificationsOf(id);

                        int rows = memberRepository.hardDeleteMemberById(id);
                        return rows > 0;
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

            if (ids.size() < chunkSize || deletedInChunk == 0) {
                break;
            }
        }

        log.info("[MemberHardDelete] 종료 - 삭제={}건, 스킵={}건", totalDeleted, totalSkipped);
    }
}
