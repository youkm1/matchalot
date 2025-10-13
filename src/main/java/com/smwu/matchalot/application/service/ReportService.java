package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.Report;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    private final ReportRepository reportRepository;
    public Mono<Report> submitReport(UserId reporterId, UserId reportedUserId,
                                     StudyMaterialId materialId, ReportType type, String description) {

        // 자기 자신 신고 방지
        if (reporterId.equals(reportedUserId)) {
            return Mono.error(new IllegalArgumentException("자기 자신을 신고할 수 없습니다"));
        }

        // 중복 신고 확인
        Mono<Boolean> duplicateCheck = materialId != null ?
                reportRepository.existsByReporterIdAndMaterialId(reporterId, materialId) :
                reportRepository.existsByReporterIdAndReportedUserId(reporterId, reportedUserId);

        return duplicateCheck
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalStateException("이미 신고한 사용자/자료입니다"));
                    }

                    Report newReport = new Report(reporterId, reportedUserId, materialId, type, description);
                    return reportRepository.save(newReport);
                })
                .doOnSuccess(report -> log.info("신고 접수: 신고자={}, 신고대상={}, 유형={}",
                        reporterId.value(), reportedUserId.value(), type))
                .doOnError(error -> log.error("신고 접수 실패: {}", error.getMessage()));
    }

    public Flux<Report> getAllReports(ReportStatus status) {
        return status != null ?
                reportRepository.findByStatus(status) :
                reportRepository.findAll();
    }

    /**
     * 관리자용: 신고 해결 처리
     */
    public Mono<Report> resolveReport(ReportId reportId, String adminNote) {
        return reportRepository.findById(reportId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("신고를 찾을 수 없습니다")))
                .map(report -> report.resolve(adminNote))
                .flatMap(reportRepository::save)
                .doOnSuccess(report -> log.info("신고 해결 처리: ID={}, 관리자메모={}",
                        reportId.value(), adminNote));
    }

    /**
     * 관리자용: 신고 기각 처리
     */
    public Mono<Report> rejectReport(ReportId reportId, String adminNote) {
        return reportRepository.findById(reportId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("신고를 찾을 수 없습니다")))
                .map(report -> report.reject(adminNote))
                .flatMap(reportRepository::save)
                .doOnSuccess(report -> log.info("신고 기각 처리: ID={}, 관리자메모={}",
                        reportId.value(), adminNote));
    }

    public Flux<Report> getReportsByReporterAndStatus(UserId reporterId, ReportStatus status) {
        return status != null ?
                reportRepository.findByReporterIdAndStatus(reporterId, status) :
                reportRepository.findByReporterId(reporterId);
    }

    /**
     * 특정 신고자의 모든 신고 조회
     */
    public Flux<Report> getReportsByReporter(UserId reporterId) {
        return reportRepository.findByReporterId(reporterId);
    }

    /**
     * 신고 상세 조회 (권한 확인 포함)
     * 신고자 본인 또는 관리자만 조회 가능
     */
    public Mono<Report> getReport(ReportId reportId, UserId requesterId) {
        return reportRepository.findById(reportId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("신고를 찾을 수 없습니다")))
                .flatMap(report -> {
                    // 신고자 본인인지 확인
                    if (report.getReporterId().equals(requesterId)) {
                        return Mono.just(report);
                    }

                    // 관리자 권한 확인이 필요한 경우 UserService를 통해 확인
                    // 여기서는 단순히 권한 없음으로 처리
                    return Mono.error(new IllegalStateException("해당 신고에 접근할 권한이 없습니다"));
                });
    }


    public Mono<Boolean> hasReportedMaterial(UserId reporterId, StudyMaterialId materialId) {
        return reportRepository.existsByReporterIdAndMaterialId(reporterId, materialId);
    }


    public Mono<Boolean> hasReportedUser(UserId reporterId, UserId reportedUserId) {
        return reportRepository.existsByReporterIdAndReportedUserId(reporterId, reportedUserId);
    }
}
