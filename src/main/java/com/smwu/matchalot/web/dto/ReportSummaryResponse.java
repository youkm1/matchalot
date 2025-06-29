package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.Report;
import java.time.LocalDateTime;

public record ReportSummaryResponse(
        Long id,
        String type,
        String typeDescription,
        String status,
        String statusDescription,
        String reportedUserNickname,
        Long materialId,
        LocalDateTime createdAt
) {
    public static ReportSummaryResponse from(Report report, String reportedUserNickname) {
        return new ReportSummaryResponse(
                report.getId().value(),
                report.getType().name(),
                report.getType().getDescription(),
                report.getStatus().name(),
                report.getStatus().getDescription(),
                reportedUserNickname,
                report.getMaterialId() != null ? report.getMaterialId().value() : null,
                report.getCreatedAt()
        );
    }
}