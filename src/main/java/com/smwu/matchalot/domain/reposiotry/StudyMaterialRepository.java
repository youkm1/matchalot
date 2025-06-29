package com.smwu.matchalot.domain.reposiotry;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StudyMaterialRepository {
    Mono<StudyMaterial> save(StudyMaterial studyMaterial);
    Mono<StudyMaterial> findById(StudyMaterialId id);
    Flux<StudyMaterial> findByUploaderId(UserId uploaderId);
    Flux<StudyMaterial> findBySubject(Subject subject);
    Flux<StudyMaterial> findBySubjectAndExamType(Subject subject, ExamType examType);
    Flux<StudyMaterial> findBySubjectAndSemester(Subject subject, Semester semester);
    Mono<Boolean> existsBySubjectAndExamTypeAndSemester(Subject subject, ExamType examType, Semester semester);
    Mono<Void> deleteById(StudyMaterialId id);
    Flux<StudyMaterial> findAll();
    Flux<StudyMaterial> findByStatus(MaterialStatus status);

    //관리자용
    Mono<Long> countByStatus(MaterialStatus status);
    Flux<StudyMaterial> findAllApproved();
    Flux<StudyMaterial> findBySubjectAndStatus(Subject subject, MaterialStatus status);
    Flux<StudyMaterial> findBySubjectAndExamTypeAndStatus(Subject subject, ExamType examType, MaterialStatus status);
}
