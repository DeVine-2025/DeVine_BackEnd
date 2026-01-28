package com.umc.devine.domain.report.event;

import com.umc.devine.domain.report.enums.ReportType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportCreatedEvent {

    private final Long reportId;
    private final String gitUrl;
    private final ReportType reportType;
}
