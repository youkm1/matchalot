package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.reposiotry.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserService userService;

    public Mono<StudyMaterial> uploadStudyMaterial(UserId uploaderId,
                                                   String title,
                                                   Subject subject,
                                                   ExamType examType,
                                                   Semester semester,
                                                   Questions questions) {
        return userService.getUserById(uploaderId)
                .filter(User::participableInMatch)  // 신뢰도 체크 (0점 이상)
                .switchIfEmpty(Mono.error(new IllegalStateException("신뢰도가 부족하여 족보를 업로드할 수 없습니다")))
                .then(validateUploadRequest(title, questions))
                .flatMap(ignored -> {
                    StudyMaterial studyMaterial = new StudyMaterial(
                            uploaderId, title, subject, examType, semester, questions
                    );



                    return studyMaterialRepository.save(studyMaterial);
                });

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
    private Mono<Void> validateUploadRequest(String title, Questions questions) {
        if (title == null || title.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("제목은 필수입니다"));
        }
        if (questions == null) {
            return Mono.error(new IllegalArgumentException("문제는 필수입니다"));
        }
        return Mono.empty();
    }

}
