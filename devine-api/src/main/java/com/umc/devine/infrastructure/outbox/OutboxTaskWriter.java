package com.umc.devine.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.outbox.repository.OutboxTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 커밋 뒤에 할 외부 작업을 아웃박스에 기록한다.
 * 호출하는 쪽 트랜잭션에 참여해야 한다(롤백되면 작업도 사라진다).
 */
@Component
@RequiredArgsConstructor
public class OutboxTaskWriter {

    private final OutboxTaskRepository outboxTaskRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxTask enqueue(OutboxTaskType type, Object payload) {
        return outboxTaskRepository.save(OutboxTask.pending(type, toJson(payload), LocalDateTime.now()));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("아웃박스 payload 직렬화 실패: " + payload.getClass().getSimpleName(), e);
        }
    }
}
