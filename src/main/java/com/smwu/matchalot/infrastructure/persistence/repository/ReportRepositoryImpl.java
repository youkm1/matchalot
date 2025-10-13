package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.domain.model.entity.Report;
import com.smwu.matchalot.domain.model.vo.ReportId;
import com.smwu.matchalot.domain.model.vo.ReportStatus;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.repository.ReportRepository;
import com.smwu.matchalot.infrastructure.persistence.ReportEntity;
import com.smwu.matchalot.infrastructure.persistence.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
class ReportRepositoryImpl implements ReportRepository {

    private final ReportR2dbcRepository r2dbcRepository;
    private final ReportMapper mapper;

    @Override
    public Mono<Report> save(Report report) {
        ReportEntity entity = mapper.toEntity(report);
        if (report.getId() == null) {
            entity.onCreate(); // BaseEntity의 onCreate 호출
        }
        return r2dbcRepository.save(entity)
                .map(mapper::toDomain)
                .doOnSuccess(saved -> log.info("신고 저장 완료: ID={}",
                        saved.getId() != null ? saved.getId().value() : "null"));
    }

    @Override
    public Mono<Report> findById(ReportId id) {
        return r2dbcRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Report> findByReporterId(UserId reporterId) {
        return r2dbcRepository.findByReporterId(reporterId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Report> findByReportedUserId(UserId reportedUserId) {
        return r2dbcRepository.findByReportedUserId(reportedUserId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Report> findByStatus(ReportStatus status) {
        return r2dbcRepository.findByStatus(status.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Report> findAll() {
        return r2dbcRepository.findAllByOrderByCreatedAtDesc()
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByReporterIdAndReportedUserId(UserId reporterId, UserId reportedUserId) {
        return r2dbcRepository.existsByReporterIdAndReportedUserId(
                reporterId.value(), reportedUserId.value());
    }

    @Override
    public Mono<Boolean> existsByReporterIdAndMaterialId(UserId reporterId, StudyMaterialId materialId) {
        return r2dbcRepository.existsByReporterIdAndMaterialId(
                reporterId.value(), materialId.value());
    }

    @Override
    public Mono<Long> countByStatus(ReportStatus status) {
        return r2dbcRepository.countByStatus(status.name());
    }

    @Override
    public Mono<Long> countByReportedUserId(UserId reportedUserId) {
        return r2dbcRepository.countByReportedUserId(reportedUserId.value());
    }

    @Override
    public Flux<Report> findByReporterIdAndStatus(UserId reporterId, ReportStatus status) {
        return r2dbcRepository.findByReporterIdAndStatus(reporterId.value(), status.name())
                .map(mapper::toDomain);
    }
}
