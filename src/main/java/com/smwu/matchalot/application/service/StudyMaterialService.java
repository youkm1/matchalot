package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.repository.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserService userService;
    private final TransactionalOperator transactionalOperator;
    private final NotificationService notificationService;

    @Transactional
    public Mono<StudyMaterial> uploadStudyMaterial(UserId uploaderId,
                                                   String title,
                                                   Subject subject,
                                                   ExamType examType,
                                                   Semester semester,
                                                   Questions questions) {
        return userService.getUserById(uploaderId)
                .doOnNext(user -> log.info("족보 업로드 시도: 이용자={}, 닉네임={}", user.getId().value(), user.getNickname()))
                .filter(User::canUploadMaterial) //ban당하지않는이상..
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
        return studyMaterialRepository.findByStatus(MaterialStatus.APPROVED);
    }

    public Flux<StudyMaterial> getStudyMaterialsBySubject(Subject subject) {
        return studyMaterialRepository.findBySubjectAndStatus(subject, MaterialStatus.APPROVED);
    }
    
    public Flux<StudyMaterial> getAllStudyMaterialsForAdmin() {
        return studyMaterialRepository.findAll();
    }
    
    public Mono<StudyMaterial> getStudyMaterialForAdmin(StudyMaterialId id) {
        if (id == null || id.value() == null) {
            return Mono.error(new IllegalArgumentException("유효하지 않은 족보의 ID"));
        }
        return studyMaterialRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("족보를 찾을 수 없습니다")));
    }

    public Flux<StudyMaterial> getApprovedStudyMaterialsBySubjectAndExamType(Subject subject, ExamType examType) {
        return studyMaterialRepository.findBySubjectAndExamTypeAndStatus(subject, examType, MaterialStatus.APPROVED);
    }

    public Flux<StudyMaterial> getStudyMaterialsBySubjectAndExamType(Subject subject, ExamType examType) {
        return studyMaterialRepository.findBySubjectAndExamTypeAndStatus(subject, examType, MaterialStatus.APPROVED);
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

    // ========== 관리자 승인 시스템 메서드들 ==========


    public Flux<StudyMaterial> getPendingMaterials() {
        return studyMaterialRepository.findByStatus(MaterialStatus.PENDING)
                .doOnNext(material -> log.info("승인 대기 족보: {}", material.getTitle()));
    }

    public Mono<StudyMaterial> approveMaterial(StudyMaterialId materialId) {
        return transactionalOperator.transactional(
            studyMaterialRepository.findById(materialId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("족보 승인 중 문제가 발생하였습니다.")))
                .flatMap(material -> {
                    //새로운 객체 생성 대신 기준 상태를 변경하는 식으로
                    StudyMaterial approvedMaterial = material.approve();

                    return studyMaterialRepository.save(approvedMaterial)
                            .doOnSuccess(saved -> log.info("족보 승인 완료: {} (ID: {})",
                                    saved.getTitle(), saved.getId().value()))
                            .flatMap(saved -> 
                                // 업로드 사용자에게 승인 알림
                                notificationService.notifyMaterialApproval(
                                    saved.getUploaderId(), 
                                    saved.getTitle(), 
                                    saved.getId().value()
                                ).then(checkForUserPromotion(saved.getUploaderId(), saved))
                            );
                })
        );
    }


    public Mono<StudyMaterial> rejectMaterial(StudyMaterialId materialId, String reason) {
        return studyMaterialRepository.findById(materialId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("족보를 찾을 수 없습니다")))
                .flatMap(material -> {
                    StudyMaterial rejectedMaterial = material.reject();

                    return studyMaterialRepository.save(rejectedMaterial)
                            .flatMap(saved -> 
                                // 업로드 사용자에게 거절 알림
                                notificationService.notifyMaterialRejection(
                                    saved.getUploaderId(),
                                    saved.getTitle(),
                                    reason,
                                    saved.getId().value()
                                ).then(Mono.just(saved))
                            )
                            .doOnSuccess(saved -> log.info("족보 거절: {} (ID: {}), 사유: {}",
                                    saved.getTitle(), saved.getId().value(), reason));
                });
    }


    private Mono<StudyMaterial> checkForUserPromotion(UserId uploaderId, StudyMaterial approvedMaterial) {
        return userService.getUserById(uploaderId)
                .flatMap(user -> {
                    // 준회원이고 첫 번째 족보가 승인된 경우 정회원으로 승격
                    if (user.isPending()) {
                        return userService.promoteToMember(uploaderId)
                                .doOnSuccess(promoted -> log.info("사용자 승격: {} → 정회원 (신뢰도 +5점)",
                                        promoted.getEmail().value()))
                                .then(Mono.just(approvedMaterial));
                    }
                    return Mono.just(approvedMaterial);
                });
    }

    /**
     * 관리자 대시보드 통계
     */
    public Mono<Map<String, Object>> getAdminStatistics() {
        return Mono.zip(
                studyMaterialRepository.countByStatus(MaterialStatus.PENDING),
                studyMaterialRepository.countByStatus(MaterialStatus.APPROVED),
                studyMaterialRepository.countByStatus(MaterialStatus.REJECTED),
                userService.countByRole(UserRole.PENDING),
                userService.countByRole(UserRole.MEMBER)
        ).map(tuple -> Map.of(
                "pendingMaterials", tuple.getT1(),
                "approvedMaterials", tuple.getT2(),
                "rejectedMaterials", tuple.getT3(),
                "pendingUsers", tuple.getT4(),
                "members", tuple.getT5()
        ));
    }
}