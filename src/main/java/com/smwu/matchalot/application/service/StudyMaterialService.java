package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.reposiotry.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserService userService;

    @Transactional
    public Mono<StudyMaterial> uploadStudyMaterial(UserId uploaderId,
                                                   String title,
                                                   Subject subject,
                                                   ExamType examType,
                                                   Semester semester,
                                                   Questions questions) {
        return userService.getUserById(uploaderId)
                .doOnNext(user -> log.info("족보 업로드 시도: 이용자={}, 닉네임={}", user.getId().value(), user.getNickname()))
                .filter(User::participableInMatch)
                .switchIfEmpty(Mono.error(new IllegalStateException("신뢰도가 부족하여 족보를 업로드할 수 없습니다")))
                .flatMap(user -> {
                    StudyMaterial studyMaterial = new StudyMaterial(
                            uploaderId, title, subject, examType, semester, questions
                    );
                    return studyMaterialRepository.save(studyMaterial);
                })
                .doOnSuccess(saved -> log.info("저장 성공 ID={}", saved != null && saved.getId() != null ? saved.getId().value() : "null"))
                .doOnError(error -> log.error("저장 실패: 오류={}", error.getMessage()));
    }
    public Flux<StudyMaterial> getAllStudyMaterials() {
        return studyMaterialRepository.findAll();
    }

    public Flux<StudyMaterial> getStudyMaterialsBySubject(Subject subject) {
        return studyMaterialRepository.findBySubject(subject);
    }

    public Flux<StudyMaterial> getStudyMaterialsBySubjectAndExamType(Subject subject, ExamType examType) {
        return studyMaterialRepository.findBySubjectAndExamType(subject, examType);
    }

    public Flux<StudyMaterial> getMyStudyMaterials(UserId uploaderId) {
        return studyMaterialRepository.findByUploaderId(uploaderId);
    }

    public Mono<StudyMaterial> getStudyMaterial(StudyMaterialId id) {
        if (id == null || id.value() == null) {
            return Mono.error(new IllegalArgumentException("유효하지 않은 족보의 ID"));
        }
        return studyMaterialRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("족보를 찾을 수 없습니다")));
    }

    public Mono<Void> deleteStudyMaterial(StudyMaterialId id, UserId requestUserId) {
        return studyMaterialRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("족보를 찾을 수 없습니다")))
                .flatMap(studyMaterial -> {
                    if (!studyMaterial.isUploadedBy(requestUserId)) {
                        return Mono.error(new IllegalStateException("본인이 업로드한 족보만 삭제할 수 있습니다"));
                    }
                    return studyMaterialRepository.deleteById(id);
                });
    }


}
