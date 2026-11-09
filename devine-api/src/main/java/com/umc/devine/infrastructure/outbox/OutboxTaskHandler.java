package com.umc.devine.infrastructure.outbox;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;

/** 다음 단계 작업은 성공한 뒤에만 기록한다. */
public interface OutboxTaskHandler {

    OutboxTaskType type();

    void handle(OutboxTask task) throws Exception;

    /** 최종 실패해도 다음 단계를 이어가야 하면 여기서 기록한다. */
    default void onFinalFailure(OutboxTask task) {
    }
}
