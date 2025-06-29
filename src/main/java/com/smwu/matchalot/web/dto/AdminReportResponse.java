package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.Report;

import java.time.LocalDateTime;

public record AdminReportResponse(
        Long id,
        String type,
        String typeDescription,
        String description,
        String status,
        String statusDescription,
        Long reporterId,
        String reporterNickname,
        Long reportedUserId,
        String reportedUserNickname,
        Long materialId,
        String materialTitle,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static AdminReportResponse from(Report report, String reporterNickname,
                                           String reportedUserNickname, String materialTitle) {
        return new AdminReportResponse(
                report.getId().value(),
                report.getType().name(),
                report.getType().getDescription(),
                report.getDescription(),
                report.getStatus().name(),
                report.getStatus().getDescription(),
                report.getReporterId().value(),
                reporterNickname,
                report.getReportedUserId().value(),
                reportedUserNickname,
                report.getMaterialId() != null ? report.getMaterialId().value() : null,
                materialTitle,
                report.getAdminNote(),
                report.getCreatedAt(),
                report.getResolvedAt()
        );
    }
}
