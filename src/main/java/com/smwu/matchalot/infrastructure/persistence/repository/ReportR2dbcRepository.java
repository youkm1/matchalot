package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.infrastructure.persistence.ReportEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReportR2dbcRepository extends R2dbcRepository<ReportEntity, Long> {
    Flux<ReportEntity> findByReporterId(Long reporterId);

    Flux<ReportEntity> findByReportedUserId(Long reportedUserId);
    Flux<ReportEntity> findByReporterIdAndStatus(Long reporterId, String status);
    default Flux<ReportEntity> findByStatus(String status) {
        return null;
    }

    Flux<ReportEntity> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(*) > 0 FROM reports WHERE reporter_id = :reporterId AND reported_user_id = :reportedUserId")
    Mono<Boolean> existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId);

    @Query("SELECT COUNT(*) > 0 FROM reports WHERE reporter_id = :reporterId AND material_id = :materialId")
    Mono<Boolean> existsByReporterIdAndMaterialId(Long reporterId, Long materialId);

    Mono<Long> countByStatus(String status);

    Mono<Long> countByReportedUserId(Long reportedUserId);

    @Query("SELECT * FROM reports WHERE status = :status ORDER BY created_at DESC")
    Flux<ReportEntity> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT * FROM reports WHERE (reporter_id = :userId OR reported_user_id = :userId) ORDER BY created_at DESC")
    Flux<ReportEntity> findByUserId(Long userId);
}
