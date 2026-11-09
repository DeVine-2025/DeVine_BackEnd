package com.umc.devine.infrastructure.outbox;

/**
 * 아웃박스에 작업을 기록했다는 신호. 커밋 뒤 바로 실행하도록 {@link OutboxTaskEventListener}가 받는다.
 * 스케줄러가 주기적으로도 실행하므로 이 신호를 놓쳐도 작업은 사라지지 않는다.
 */
public record OutboxTaskEnqueuedEvent() {
}
