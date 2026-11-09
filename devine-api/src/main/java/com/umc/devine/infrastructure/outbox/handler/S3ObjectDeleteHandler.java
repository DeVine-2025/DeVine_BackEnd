package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import com.umc.devine.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
