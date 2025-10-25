package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.repository.StudyMaterialRepository;
import com.smwu.matchalot.infrastructure.persistence.StudyMaterialEntity;
import com.smwu.matchalot.infrastructure.persistence.mapper.StudyMaterialMapper;
import com.smwu.matchalot.web.dto.StudyMaterialSummaryResponse;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
@Slf4j
@RequiredArgsConstructor
public class StudyMaterialRepositoryImpl implements StudyMaterialRepository {
    private final StudyMaterialR2dbcRepository r2dbcRepository;
    private final StudyMaterialMapper mapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<StudyMaterial> save(StudyMaterial studyMaterial) {
        log.info("입력 도메인: title={}, subject={}, id={}",
                studyMaterial.getTitle(),
                studyMaterial.getSubject().name(),
                studyMaterial.getId());

        StudyMaterialEntity entity = mapper.toEntity(studyMaterial);

        log.info("변환된 엔티티: id={}, title={}, uploaderId={}, questionsJson 길이={}",
                entity.getId(),
                entity.getTitle(),
                entity.getUploaderId(),
                entity.getQuestionsJson() != null ? entity.getQuestionsJson().asString() : "null");

        //entity.onCreate();

        log.info("onCreate 후: createdAt={}, updatedAt={}", entity.getCreatedAt(), entity.getUpdatedAt());

        if (studyMaterial.getId() == null) {
            entity.setTimestamps();
        } else {
            entity.setUpdatedAt();
        }
        return r2dbcRepository.save(entity)
                .map(mapper::toDomain)
                .doOnSuccess(saved -> log.info("StudyMaterial 저장 완료: ID={}, title={}, status={}",
                        saved.getId() != null ? saved.getId().value() : "null",
                        saved.getTitle(),
                        saved.getStatus()))
                .doOnError(error -> log.error("StudyMaterial 저장 실패: title={}, error={}",
                        studyMaterial.getTitle(), error.getMessage()));


    }

    @Override
    public Mono<StudyMaterial> findById(StudyMaterialId id) {
        return r2dbcRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findBySubject(Subject subject) {
        return r2dbcRepository.findBySubject(subject.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findBySubjectAndExamType(Subject subject, ExamType examType) {
        return r2dbcRepository.findBySubjectAndExamType(subject.name(), examType.type())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findByUploaderId(UserId uploaderId) {
        return r2dbcRepository.findByUploaderId(uploaderId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findBySubjectAndSemester(Subject subject, Semester semester) {
        return r2dbcRepository.findBySubjectAndYearAndSeason(
                        subject.name(), semester.year(), semester.season())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findByUploaderIdAndSubjectAndExamType(UserId uploaderId, Subject subject, ExamType examType) {
        return r2dbcRepository.findByUploaderIdAndSubjectAndExamType(
                uploaderId.value(), subject.name(), examType.type())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsBySubjectAndExamTypeAndSemester(Subject subject, ExamType examType, Semester semester) {
        return r2dbcRepository.existsBySubjectAndExamTypeAndYearAndSeason(
                subject.name(), examType.type(), semester.year(), semester.season());
    }

    @Override
    public Mono<Void> deleteById(StudyMaterialId id) {
        return r2dbcRepository.deleteById(id.value());
    }

    @Override
    public Flux<StudyMaterial> findAll() {
        return r2dbcRepository.findAllByOrderByCreatedAtDesc() // 최신순
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findByStatus(MaterialStatus status) {
        return r2dbcRepository.findByStatus(status.name())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countByStatus(MaterialStatus status) {
        return r2dbcRepository.countByStatus(status.name());
    }

    @Override
    public Flux<StudyMaterial> findAllApproved() {
        return r2dbcRepository.findAllApproved()
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findBySubjectAndStatus(Subject subject, MaterialStatus status) {
        return r2dbcRepository.findBySubjectAndStatus(subject.name(), status.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<StudyMaterial> findBySubjectAndExamTypeAndStatus(Subject subject, ExamType examType, MaterialStatus status) {
        return r2dbcRepository.findBySubjectAndExamTypeAndStatus(
                        subject.name(), examType.type(), status.name())
                .map(mapper::toDomain);
    }

    // JOIN 메서드들 구현 - N+1 문제 해결
    @Override
    public Flux<StudyMaterialSummaryResponse> findAllWithUploader() {
        return databaseClient.sql("""
                SELECT 
                    m.id, m.uploader_id, m.title, m.subject, m.exam_type, 
                    m.year, m.season, m.question_count, m.created_at, m.temp_pdf_data,
                    u.trust_score
                FROM study_material m
                INNER JOIN users u ON m.uploader_id = u.id
                WHERE m.status = 'APPROVED'
                ORDER BY m.created_at DESC
                """)
                .map((row, metadata) -> StudyMaterialSummaryResponse.fromJoinResult(
                        row.get("id", Long.class),
                        row.get("uploader_id", Long.class),
                        row.get("subject", String.class),
                        row.get("exam_type", String.class),
                        row.get("year", Integer.class),
                        row.get("season", String.class),
                        row.get("title", String.class),
                        row.get("question_count", Integer.class),
                        row.get("trust_score", Integer.class),
                        row.get("created_at", LocalDateTime.class),
                        row.get("temp_pdf_data", String.class)
                ))
                .all();
    }

    @Override
    public Flux<StudyMaterialSummaryResponse> findAllWithUploaderForAdmin() {
        return databaseClient.sql("""
                SELECT 
                    m.id, m.uploader_id, m.title, m.subject, m.exam_type, 
                    m.year, m.season, m.question_count, m.created_at, m.temp_pdf_data,
                    u.trust_score
                FROM study_material m
                INNER JOIN users u ON m.uploader_id = u.id
                ORDER BY m.created_at DESC
                """)
                .map((row, metadata) -> StudyMaterialSummaryResponse.fromJoinResult(
                        row.get("id", Long.class),
                        row.get("uploader_id", Long.class),
                        row.get("subject", String.class),
                        row.get("exam_type", String.class),
                        row.get("year", Integer.class),
                        row.get("season", String.class),
                        row.get("title", String.class),
                        row.get("question_count", Integer.class),
                        row.get("trust_score", Integer.class),
                        row.get("created_at", LocalDateTime.class),
                        row.get("temp_pdf_data", String.class)
                ))
                .all();
    }

    @Override
    public Flux<StudyMaterialSummaryResponse> findBySubjectWithUploader(Subject subject) {
        return databaseClient.sql("""
                SELECT 
                    m.id, m.uploader_id, m.title, m.subject, m.exam_type, 
                    m.year, m.season, m.question_count, m.created_at, m.temp_pdf_data,
                    u.trust_score
                FROM study_material m
                INNER JOIN users u ON m.uploader_id = u.id
                WHERE m.subject = :subject AND m.status = 'APPROVED'
                ORDER BY m.created_at DESC
                """)
                .bind("subject", subject.name())
                .map((row, metadata) -> StudyMaterialSummaryResponse.fromJoinResult(
                        row.get("id", Long.class),
                        row.get("uploader_id", Long.class),
                        row.get("subject", String.class),
                        row.get("exam_type", String.class),
                        row.get("year", Integer.class),
                        row.get("season", String.class),
                        row.get("title", String.class),
                        row.get("question_count", Integer.class),
                        row.get("trust_score", Integer.class),
                        row.get("created_at", LocalDateTime.class),
                        row.get("temp_pdf_data", String.class)
                ))
                .all();
    }

    @Override
    public Flux<StudyMaterialSummaryResponse> findBySubjectAndExamTypeWithUploader(Subject subject, ExamType examType) {
        return databaseClient.sql("""
                SELECT 
                    m.id, m.uploader_id, m.title, m.subject, m.exam_type, 
                    m.year, m.season, m.question_count, m.created_at, m.temp_pdf_data,
                    u.trust_score
                FROM study_material m
                INNER JOIN users u ON m.uploader_id = u.id
                WHERE m.subject = :subject AND m.exam_type = :examType AND m.status = 'APPROVED'
                ORDER BY m.created_at DESC
                """)
                .bind("subject", subject.name())
                .bind("examType", examType.type())
                .map((row, metadata) -> StudyMaterialSummaryResponse.fromJoinResult(
                        row.get("id", Long.class),
                        row.get("uploader_id", Long.class),
                        row.get("subject", String.class),
                        row.get("exam_type", String.class),
                        row.get("year", Integer.class),
                        row.get("season", String.class),
                        row.get("title", String.class),
                        row.get("question_count", Integer.class),
                        row.get("trust_score", Integer.class),
                        row.get("created_at", LocalDateTime.class),
                        row.get("temp_pdf_data", String.class)
                ))
                .all();
    }

    @Override
    public Flux<StudyMaterialSummaryResponse> findByUploaderIdWithUploader(UserId uploaderId) {
        return databaseClient.sql("""
                SELECT 
                    m.id, m.uploader_id, m.title, m.subject, m.exam_type, 
                    m.year, m.season, m.question_count, m.created_at, m.temp_pdf_data,
                    u.trust_score
                FROM study_material m
                INNER JOIN users u ON m.uploader_id = u.id
                WHERE m.uploader_id = :uploaderId
                ORDER BY m.created_at DESC
                """)
                .bind("uploaderId", uploaderId.value())
                .map((row, metadata) -> StudyMaterialSummaryResponse.fromJoinResult(
                        row.get("id", Long.class),
                        row.get("uploader_id", Long.class),
                        row.get("subject", String.class),
                        row.get("exam_type", String.class),
                        row.get("year", Integer.class),
                        row.get("season", String.class),
                        row.get("title", String.class),
                        row.get("question_count", Integer.class),
                        row.get("trust_score", Integer.class),
                        row.get("created_at", LocalDateTime.class),
                        row.get("temp_pdf_data", String.class)
                ))
                .all();
    }

}