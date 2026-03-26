package com.umc.devine.domain.report.event;

import lombok.Builder;
import lombok.Getter;

/**
 * 리포트 완료/실패 시 알림을 위한 도메인 이벤트
 * Report → Notification 간 결합을 제거하기 위해 사용
 */
@Getter
@Builder
public class ReportNotificationEvent {

    private final Long receiverId;
    private final Long reportId;
    private final String gitUrl;
    private final boolean success;
}
