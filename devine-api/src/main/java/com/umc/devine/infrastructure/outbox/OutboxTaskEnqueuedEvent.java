package com.umc.devine.infrastructure.outbox;

/** 커밋 직후 아웃박스를 바로 실행하기 위한 신호. 놓쳐도 스케줄러가 다시 잡는다. */
public record OutboxTaskEnqueuedEvent() {
}
