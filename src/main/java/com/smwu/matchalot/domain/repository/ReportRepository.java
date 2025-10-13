package com.smwu.matchalot.domain.repository;

import com.smwu.matchalot.domain.model.entity.Report;
import com.smwu.matchalot.domain.model.vo.ReportId;
import com.smwu.matchalot.domain.model.vo.ReportStatus;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReportRepository {
    Mono<Report> save(Report report);
    Mono<Report> findById(ReportId id);
    Flux<Report> findByReporterId(UserId reporterId);
    Flux<Report> findByReportedUserId(UserId reportedUserId);
    Flux<Report> findByStatus(ReportStatus status);
    Flux<Report> findAll();
    Mono<Boolean> existsByReporterIdAndReportedUserId(UserId reporterId, UserId reportedUserId);
    Mono<Boolean> existsByReporterIdAndMaterialId(UserId reporterId, StudyMaterialId materialId);
    Mono<Long> countByStatus(ReportStatus status);
    Mono<Long> countByReportedUserId(UserId reportedUserId);
    Flux<Report> findByReporterIdAndStatus(UserId reporterId, ReportStatus status);
}
