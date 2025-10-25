package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.MatchService;
import com.smwu.matchalot.application.service.StudyMaterialService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.repository.MatchRepository;
import com.smwu.matchalot.domain.repository.StudyMaterialRepository;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/study-materials")
@RequiredArgsConstructor
@Slf4j
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;
    private final UserService userService;
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final StudyMaterialRepository studyMaterialRepository;

    @PostMapping
    public Mono<ResponseEntity<StudyMaterialResponse>> uploadStudyMaterial(
            @Valid @RequestBody StudyMaterialUploadRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        log.info("email: {}", email);
        return userService.getUserByEmail(userEmail)
                .flatMap(user -> studyMaterialService.uploadStudyMaterial(
                        user.getId(),
                        request.title(),
                        request.getSubjectVO(),
                        request.getExamTypeVO(),
                        request.getSemesterVO(),
                        request.getQuestionsVO(),
                        request.tempPdfData()
                ))
                .flatMap(studyMaterial -> {
                    // 업로더 신뢰도를 가져와서 응답 생성
                    return userService.getUserById(studyMaterial.getUploaderId())
                            .map(uploader -> StudyMaterialResponse.from(studyMaterial, uploader.getTrustScore().value()));
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
            @RequestParam(value = "examType", required = false) String examType,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        // 관리자인지 확인
        Mono<Boolean> isAdminCheck = oauth2User != null ? 
            userService.isAdminByEmail(Email.of(oauth2User.getAttribute("email"))) :
            Mono.just(false);

        return isAdminCheck.flatMapMany(isAdmin -> {
            // JOIN 쿼리로 N+1 문제 해결 (1번 쿼리)
            if (subject != null && examType != null) {
                return studyMaterialRepository.findBySubjectAndExamTypeWithUploader(
                        Subject.of(subject), ExamType.of(examType));
            } else if (subject != null) {
                return studyMaterialRepository.findBySubjectWithUploader(Subject.of(subject));
            } else {
                // 관리자는 모든 상태, 일반 사용자는 승인된 것만
                return isAdmin ?
                    studyMaterialRepository.findAllWithUploaderForAdmin() :
                    studyMaterialRepository.findAllWithUploader();
            }
        });
    }

    @GetMapping("/{materialId}")
    public Mono<ResponseEntity<StudyMaterialResponse>> getStudyMaterial(
            @PathVariable("materialId") Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        StudyMaterialId id = StudyMaterialId.of(materialId);
        
        // 로그인하지 않은 경우 - 미리보기만 제공
        if (oauth2User == null) {
            return studyMaterialService.getStudyMaterial(id)
                    .flatMap(this::toPreviewResponse)
                    .map(ResponseEntity::ok)
                    .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
        }
        
        // 로그인한 경우 - 매칭 여부에 따라 전체/미리보기 제공
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> {
                    // 관리자인 경우 모든 족보에 대해 전체 접근 권한 (승인 상태 무관)
                    return userService.isAdminByEmail(userEmail)
                            .flatMap(isAdmin -> {
                                if (isAdmin) {
                                    return studyMaterialService.getStudyMaterialForAdmin(id)
                                            .flatMap(this::toFullResponse);
                                }
                                
                                // 일반 사용자는 본인 업로드 또는 매칭 여부에 따라 접근 권한 결정
                                return studyMaterialService.getStudyMaterial(id)
                                        .flatMap(material -> {
                                            // 본인이 업로드한 족보인지 확인
                                            if (material.isUploadedBy(user.getId())) {
                                                return toFullResponse(material);
                                            }
                                            
                                            // 매칭으로 접근 권한이 있는지 확인
                                            return matchRepository.hasAccessToMaterial(user.getId(), id)
                                                    .flatMap(hasAccess -> {
                                                        if (hasAccess) {
                                                            return toFullResponse(material);
                                                        } else {
                                                            return toPreviewResponse(material);
                                                        }
                                                    });
                                        });
                            });
                })
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }


    @GetMapping("/my")
    public Flux<StudyMaterialSummaryResponse> getMyStudyMaterials(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Flux.empty();
        }
        
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> {
                    // JOIN 쿼리로 N+1 문제 해결 (1번 쿼리)
                    return studyMaterialRepository.findByUploaderIdWithUploader(user.getId());
                });
    }

    @DeleteMapping("/{materialId}")
    public Mono<ResponseEntity<Map<String, String>>> deleteStudyMaterial(
            @PathVariable("materialId") Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다")));
        }
        
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
                Subject.COMPUTER_NETWORK_1.name(),
                Subject.SOCIAL_MEDIA_UNDERSTANDING.name(),
                Subject.KOREAN_CULTURE_UNDERSTANDING.name(),
                Subject.CLASSIC_FIELD_STORY.name(),
                Subject.DIGITAL_PHILOSOPHY.name(),
                Subject.WESTERN_HISTORY_CULTURE.name(),
                Subject.COMPUTER_MATH_HJ.name(),
                Subject.COMPUTER_MATH_YO.name()
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

    // Batch processing method for N+1 optimization
    private Flux<StudyMaterialSummaryResponse> toSummaryResponse(Flux<StudyMaterial> materialsFlux) {
        return materialsFlux
                .collectList()
                .flatMapMany(materials -> {
                    if (materials.isEmpty()) {
                        return Flux.empty();
                    }
                    
                    // Extract all uploader IDs
                    Set<UserId> uploaderIds = materials.stream()
                            .map(StudyMaterial::getUploaderId)
                            .collect(Collectors.toSet());
                    
                    // Batch fetch all users (1 query instead of N)
                    return userService.getUsersByIds(uploaderIds)
                            .collectMap(com.smwu.matchalot.domain.model.entity.User::getId)  // Map<UserId, User>
                            .flatMapMany(userMap -> 
                                Flux.fromIterable(materials)
                                    .map(material -> {
                                        com.smwu.matchalot.domain.model.entity.User uploader = userMap.get(material.getUploaderId());
                                        int trustScore = uploader != null ? uploader.getTrustScore().value() : 0;
                                        return StudyMaterialSummaryResponse.from(material, trustScore);
                                    })
                            );
                });
    }

    private Mono<StudyMaterialResponse> toFullResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialResponse.from(studyMaterial, uploader.getTrustScore().value()))
                .switchIfEmpty(Mono.just(StudyMaterialResponse.from(studyMaterial, 0)));
    }

    private Mono<StudyMaterialResponse> toPreviewResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialResponse.fromPreview(studyMaterial, uploader.getTrustScore().value()))
                .switchIfEmpty(Mono.just(StudyMaterialResponse.fromPreview(studyMaterial, 0)));
    }
}