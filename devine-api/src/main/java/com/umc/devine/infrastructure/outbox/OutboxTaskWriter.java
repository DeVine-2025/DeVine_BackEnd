package com.umc.devine.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.outbox.repository.OutboxTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxTaskWriter {

    private final OutboxTaskRepository outboxTaskRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxTask enqueue(OutboxTaskType type, Object payload) {
        OutboxTask task = outboxTaskRepository.save(OutboxTask.pending(type, toJson(payload), LocalDateTime.now()));
        eventPublisher.publishEvent(new OutboxTaskEnqueuedEvent());
        return task;
    }

    public <T> T readPayload(OutboxTask task, Class<T> type) {
        try {
            return objectMapper.readValue(task.getPayload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("아웃박스 payload 역직렬화 실패: taskId=" + task.getId(), e);
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("아웃박스 payload 직렬화 실패: " + payload.getClass().getSimpleName(), e);
        }
    }
}
