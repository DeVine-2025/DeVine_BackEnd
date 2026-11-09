package com.umc.devine.infrastructure.outbox;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;

/**
 * 아웃박스 작업 종류별 실행기. {@link OutboxTaskProcessor}가 작업마다 새 트랜잭션 안에서 부른다.
 * 실패는 예외로 알린다. 다음 단계 작업은 성공한 뒤에만 기록한다(실패 시 함께 롤백되지 않도록).
 */
public interface OutboxTaskHandler {

    OutboxTaskType type();

    void handle(OutboxTask task) throws Exception;

    /** 재시도 한도를 넘겨 최종 실패했을 때. 다음 단계를 이어서 실행해야 하면 여기서 기록한다. */
    default void onFinalFailure(OutboxTask task) {
    }
}
