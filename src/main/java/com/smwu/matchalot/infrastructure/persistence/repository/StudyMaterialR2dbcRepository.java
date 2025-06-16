package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.infrastructure.persistence.StudyMaterialEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StudyMaterialR2dbcRepository extends R2dbcRepository<StudyMaterialEntity, Long> {

    Flux<StudyMaterialEntity> findBySubject(String subject);

    Flux<StudyMaterialEntity> findBySubjectAndExamType(String subject, String examType);

    Flux<StudyMaterialEntity> findByUploaderId(Long uploaderId);

    Flux<StudyMaterialEntity> findBySubjectAndYearAndSeason(String subject, Integer year, String season);

    Mono<Boolean> existsBySubjectAndExamTypeAndYearAndSeason(String subject, String examType, Integer year, String season);

    Flux<StudyMaterialEntity> findAllByOrderByCreatedAtDesc();

}
