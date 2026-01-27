package com.umc.devine.domain.report.event;

import com.umc.devine.domain.report.entity.DevReport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ReportCreatedEvent {

    private final DevReport report;
}
