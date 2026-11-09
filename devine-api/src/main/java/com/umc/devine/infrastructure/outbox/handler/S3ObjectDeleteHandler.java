package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import com.umc.devine.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 탈퇴 외부 정리 3단계: 프로필, 프로젝트, 에디터 이미지 S3 객체 삭제(01-1 FUNC-MEM-005 3번).
 * S3 삭제는 없는 키도 성공이라, 재시도 때 이미 지운 키를 다시 지워도 된다.
 */
@Component
@RequiredArgsConstructor
public class S3ObjectDeleteHandler implements OutboxTaskHandler {

    private final S3Service s3Service;
    private final OutboxTaskWriter outboxTaskWriter;

    @Override
    public OutboxTaskType type() {
        return OutboxTaskType.S3_OBJECT_DELETE;
    }

    @Override
    public void handle(OutboxTask task) {
        MemberExternalCleanupPayload payload = outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class);
        if (payload.s3Keys() == null) {
            return;
        }
        payload.s3Keys().forEach(s3Service::deleteObject);
    }
}
