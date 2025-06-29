package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.Report;

import java.time.LocalDateTime;

public record ReportDetailResponse(
        Long id,
        String type,
        String typeDescription,
        String description,
        String status,
        String statusDescription,
        String reportedUserNickname,
        Long materialId,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static ReportDetailResponse from(Report report, String reportedUserNickname) {
        return new ReportDetailResponse(
                report.getId().value(),
                report.getType().name(),
                report.getType().getDescription(),
                report.getDescription(),
                report.getStatus().name(),
                report.getStatus().getDescription(),
                reportedUserNickname,
                report.getMaterialId() != null ? report.getMaterialId().value() : null,
                report.getAdminNote(),
                report.getCreatedAt(),
                report.getResolvedAt()
        );
    }
}