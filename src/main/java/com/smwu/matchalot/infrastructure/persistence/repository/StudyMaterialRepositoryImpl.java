package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.reposiotry.StudyMaterialRepository;
import com.smwu.matchalot.infrastructure.persistence.StudyMaterialEntity;
import com.smwu.matchalot.infrastructure.persistence.mapper.StudyMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface StudyMaterialR2dbcRepository extends R2dbcRepository<StudyMaterialEntity, Long> {
    Flux<StudyMaterialEntity> findBySubject(String subject);
    Flux<StudyMaterialEntity> findBySubjectAndExamType(String subject, String examType);
    Flux<StudyMaterialEntity> findByUploaderId(Long uploaderId);
    Flux<StudyMaterialEntity> findBySubjectAndYearAndSeason(String subject, Integer year, String season);
    Mono<Boolean> existsBySubjectAndExamTypeAndYearAndSeason(String subject, String examType, Integer year, String season);
    Flux<StudyMaterialEntity> findAllByOrderByCreatedAtDesc();
}
@Repository
@RequiredArgsConstructor
public class StudyMaterialRepositoryImpl implements StudyMaterialRepository {
    private final StudyMaterialR2dbcRepository r2dbcRepository;
    private final StudyMaterialMapper mapper;
    @Override
    public Mono<StudyMaterial> save(StudyMaterial studyMaterial) {
        StudyMaterialEntity entity = mapper.toEntity(studyMaterial);
        entity.onCreate();
        return r2dbcRepository.save(entity)
                .map(mapper::toDomain);
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
}
