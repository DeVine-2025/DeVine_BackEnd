package com.umc.devine.infrastructure.outbox;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;

public interface OutboxTaskHandler {

    OutboxTaskType type();

    void handle(OutboxTask task) throws Exception;

    default void onFinalFailure(OutboxTask task) {
    }
}
