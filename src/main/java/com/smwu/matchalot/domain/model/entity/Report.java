package com.smwu.matchalot.domain.model.entity;

import com.smwu.matchalot.domain.model.vo.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class Report {
    private final ReportId id;
    private final UserId reporterId;        // 신고자
    private final UserId reportedUserId;    // 신고당한 사용자
    private final StudyMaterialId materialId; // 신고된 자료 (선택적)
    private final ReportType type;
    private final String description;
    private final ReportStatus status;
    private final String adminNote;         // 관리자 처리 메모
    private final LocalDateTime createdAt;
    private final LocalDateTime resolvedAt;

    public Report(UserId reporterId, UserId reportedUserId, StudyMaterialId materialId,
                  ReportType type, String description) {
        this(null, reporterId, reportedUserId, materialId, type, description,
                ReportStatus.PENDING, null, LocalDateTime.now(), null);
    }

    // 관리자 처리
    public Report resolve(String adminNote) {
        if (status != ReportStatus.PENDING) {
            throw new IllegalStateException("처리 가능한 상태가 아닙니다");
        }
        return new Report(id, reporterId, reportedUserId, materialId, type, description,
                ReportStatus.RESOLVED, adminNote, createdAt, LocalDateTime.now());
    }

    public Report reject(String adminNote) {
        if (status != ReportStatus.PENDING) {
            throw new IllegalStateException("처리 가능한 상태가 아닙니다");
        }
        return new Report(id, reporterId, reportedUserId, materialId, type, description,
                ReportStatus.REJECTED, adminNote, createdAt, LocalDateTime.now());
    }


    public boolean isReportedBy(UserId userId) {
        return reporterId.equals(userId);
    }

    public boolean isTargeting(UserId userId) {
        return reportedUserId.equals(userId);
    }
}
