package com.umc.devine.domain.outbox.repository;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxTaskRepository extends JpaRepository<OutboxTask, Long> {

    @Query("SELECT t.id FROM OutboxTask t " +
           "WHERE t.status = :status AND t.nextAttemptAt <= :now " +
           "ORDER BY t.nextAttemptAt ASC, t.id ASC")
    List<Long> findDueIds(@Param("status") OutboxTaskStatus status,
                          @Param("now") LocalDateTime now,
                          Pageable pageable);

    /**
     * 실행할 작업 하나를 잠근다. 여러 서버(블루그린 배포)가 같은 작업을 동시에 실행하지 않도록 SKIP LOCKED로 건너뛴다.
     * 다른 서버가 이미 처리했거나 처리 중이면 비어 있다.
     */
    @Query(value = "SELECT * FROM outbox_task WHERE outbox_task_id = :id AND status = 'PENDING' AND next_attempt_at <= :now " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<OutboxTask> lockDue(@Param("id") Long id, @Param("now") LocalDateTime now);
}
