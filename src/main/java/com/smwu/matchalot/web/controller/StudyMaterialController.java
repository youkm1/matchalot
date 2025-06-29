package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.MatchService;
import com.smwu.matchalot.application.service.StudyMaterialService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/study-materials")
@RequiredArgsConstructor
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;
    private final UserService userService;
    private final MatchService matchService;

    @PostMapping
    public Mono<ResponseEntity<StudyMaterialResponse>> uploadStudyMaterial(
            @Valid @RequestBody StudyMaterialUploadRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> studyMaterialService.uploadStudyMaterial(
                        user.getId(),
                        request.title(),
                        request.getSubjectVO(),
                        request.getExamTypeVO(),
                        request.getSemesterVO(),
                        request.getQuestionsVO()
                ))
                .flatMap(studyMaterial -> {
                    // 업로더 닉네임을 가져와서 응답 생성
                    return userService.getUserById(studyMaterial.getUploaderId())
                            .map(uploader -> StudyMaterialResponse.from(studyMaterial, uploader.getNickname(), uploader.getTrustScore().value()));
                })
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @GetMapping
    public Flux<StudyMaterialSummaryResponse> getAllStudyMaterials(

            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "examType", required = false) String examType) {

        if (subject != null && examType != null) {
            // 과목과 시험 유형으로 필터링
            return studyMaterialService.getStudyMaterialsBySubjectAndExamType(
                            Subject.of(subject), ExamType.of(examType))
                    .flatMap(this::toSummaryResponse);
        } else if (subject != null) {
            // 과목으로만 필터링
            return studyMaterialService.getStudyMaterialsBySubject(Subject.of(subject))
                    .flatMap(this::toSummaryResponse);
        } else {
            // 모든 족보 조회
            return studyMaterialService.getAllStudyMaterials()
                    .flatMap(this::toSummaryResponse);
        }
    }

    @GetMapping("/{materialId}")
    public Mono<ResponseEntity<StudyMaterialResponse>> getStudyMaterial(
            @PathVariable("materialId") Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId id = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> {
                    return matchService.hasCompletedMatch(user.getId(), id)
                            .flatMap(hasAccess -> {
                                if (hasAccess) {
                                    return studyMaterialService.getStudyMaterial(id)
                                            .flatMap(this::toFullResponse);
                                } else {
                                    return studyMaterialService.getStudyMaterial(id)
                                            .flatMap(this::toPreviewResponse);
                                }
                            });
                })
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }


    @GetMapping("/my")
    public Flux<StudyMaterialSummaryResponse> getMyStudyMaterials(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> studyMaterialService.getMyStudyMaterials(user.getId()))
                .flatMap(this::toSummaryResponse);
    }

    @DeleteMapping("/{materialId}")
    public Mono<ResponseEntity<Map<String, String>>> deleteStudyMaterial(
            @PathVariable("materialId") Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId id = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> studyMaterialService.deleteStudyMaterial(id, user.getId()))
                .then(Mono.just(ResponseEntity.ok(Map.of("message", "족보가 삭제되었습니다."))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of("error", "족보를 찾을 수 없습니다.")))
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "본인이 업로드한 족보만 삭제할 수 있습니다.")));
    }


    @GetMapping("/subjects")
    public Mono<ResponseEntity<Map<String, Object>>> getAvailableSubjects() {
        // 현재는 상수로 정의된 과목들을 반환
        var subjects = java.util.List.of(
                Subject.PROGRAMMING_LANGUAGES.name(),
                Subject.COMPUTER_ARCHITECTURE.name()
        );

        return Mono.just(ResponseEntity.ok(Map.of(
                "subjects", subjects,
                "message", "사용 가능한 과목 목록입니다."
        )));
    }


    @GetMapping("/exam-types")
    public Mono<ResponseEntity<Map<String, Object>>> getAvailableExamTypes() {
        var examTypes = java.util.List.of(
                ExamType.MIDTERM.type(),
                ExamType.FINAL.type()
        );

        return Mono.just(ResponseEntity.ok(Map.of(
                "examTypes", examTypes,
                "message", "사용 가능한 시험 유형 목록입니다."
        )));
    }

    private Mono<StudyMaterialSummaryResponse> toSummaryResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialSummaryResponse.from(
                        studyMaterial,
                        uploader.getNickname(),
                        uploader.getTrustScore().value()))
                .switchIfEmpty(Mono.just(StudyMaterialSummaryResponse.from(studyMaterial, "알 수 없음",0)));
    }

    private Mono<StudyMaterialResponse> toFullResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialResponse.from(studyMaterial, uploader.getNickname(), uploader.getTrustScore().value()))
                .switchIfEmpty(Mono.just(StudyMaterialResponse.from(studyMaterial, "알 수 없음", 0)));
    }

    private Mono<StudyMaterialResponse> toPreviewResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialResponse.from(studyMaterial, uploader.getNickname(), uploader.getTrustScore().value()))
                .switchIfEmpty(Mono.just(StudyMaterialResponse.from(studyMaterial, "알 수 없음", 0)));
    }
}