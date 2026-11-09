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

    @Query(value = "SELECT * FROM outbox_task WHERE outbox_task_id = :id AND status = 'PENDING' AND next_attempt_at <= :now " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<OutboxTask> lockDue(@Param("id") Long id, @Param("now") LocalDateTime now);
}
