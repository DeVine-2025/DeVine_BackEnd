package com.umc.devine.domain.report.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportCreatedEvent {

    private final Long mainReportId;
    private final Long detailReportId;
    private final String gitUrl;
    private final String clerkId;
    private final Long memberId;
}
