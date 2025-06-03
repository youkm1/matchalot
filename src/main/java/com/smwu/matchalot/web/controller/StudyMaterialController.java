package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.StudyMaterialService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.web.dto.StudyMaterialCreateRequest;
import com.smwu.matchalot.web.dto.StudyMaterialResponse;
import com.smwu.matchalot.web.dto.StudyMaterialUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/study-materials")
@RequiredArgsConstructor
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;
    private final UserService userService;

    @PostMapping
    public Mono<ResponseEntity<StudyMaterialResponse>> createStudyMaterial(
            @RequestBody StudyMaterialCreateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> {
                    if (!user.participableInMatch()) {
                        return Mono.error(new IllegalStateException("신뢰도가 낮아 스터디 자료를 등록할 수 없습니다."));
                    }
                    return studyMaterialService.createStudyMaterial(
                            user.getId(),
                            request.title(),
                            request.description(),
                            request.subject(),
                            request.difficulty(),
                            request.fileUrl()
                    );
                })
                .map(this::toStudyMaterialResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @GetMapping
    public Flux<StudyMaterialResponse> getAllStudyMaterials(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return studyMaterialService.getStudyMaterials(subject, difficulty, page, size)
                .map(this::toStudyMaterialResponse);
    }

    @GetMapping("/{materialId}")
    public Mono<ResponseEntity<StudyMaterialResponse>> getStudyMaterial(
            @PathVariable Long materialId) {

        StudyMaterialId id = StudyMaterialId.of(materialId);

        return studyMaterialService.getStudyMaterialById(id)
                .map(this::toStudyMaterialResponse)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/my")
    public Flux<StudyMaterialResponse> getMyStudyMaterials(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> studyMaterialService.getStudyMaterialsByUserId(user.getId()))
                .map(this::toStudyMaterialResponse);
    }

    @PutMapping("/{materialId}")
    public Mono<ResponseEntity<StudyMaterialResponse>> updateStudyMaterial(
            @PathVariable Long materialId,
            @RequestBody StudyMaterialUpdateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId id = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> studyMaterialService.updateStudyMaterial(
                        id,
                        user.getId(),
                        request.title(),
                        request.description(),
                        request.subject(),
                        request.difficulty(),
                        request.fileUrl()
                ))
                .map(this::toStudyMaterialResponse)
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().build())
                .onErrorReturn(SecurityException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build())
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{materialId}")
    public Mono<ResponseEntity<Map<String, String>>> deleteStudyMaterial(
            @PathVariable Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId id = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> studyMaterialService.deleteStudyMaterial(id, user.getId()))
                .then(Mono.just(ResponseEntity.ok(Map.of("message", "스터디 자료가 삭제되었습니다."))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of("error", "잘못된 요청입니다.")))
                .onErrorReturn(SecurityException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "권한이 없습니다.")))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound()
                        .body(Map.of("error", "스터디 자료를 찾을 수 없습니다."))));
    }

    @PostMapping("/{materialId}/like")
    public Mono<ResponseEntity<Map<String, Object>>> likeStudyMaterial(
            @PathVariable Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId id = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> studyMaterialService.likeStudyMaterial(id, user.getId()))
                .map(likeCount -> ResponseEntity.ok(Map.of(
                        "message", "좋아요가 추가되었습니다.",
                        "likeCount", likeCount
                )))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of("error", "이미 좋아요를 누른 자료입니다.")))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound()
                        .body(Map.of("error", "스터디 자료를 찾을 수 없습니다."))));
    }

    @DeleteMapping("/{materialId}/like")
    public Mono<ResponseEntity<Map<String, Object>>> unlikeStudyMaterial(
            @PathVariable Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId id = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> studyMaterialService.unlikeStudyMaterial(id, user.getId()))
                .map(likeCount -> ResponseEntity.ok(Map.of(
                        "message", "좋아요가 취소되었습니다.",
                        "likeCount", likeCount
                )))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of("error", "좋아요를 누르지 않은 자료입니다.")))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound()
                        .body(Map.of("error", "스터디 자료를 찾을 수 없습니다."))));
    }

    @GetMapping("/subjects")
    public Mono<ResponseEntity<Map<String, Object>>> getAvailableSubjects() {
        return studyMaterialService.getAvailableSubjects()
                .collectList()
                .map(subjects -> ResponseEntity.ok(Map.of(
                        "subjects", subjects,
                        "message", "사용 가능한 과목 목록입니다."
                )));
    }

    @GetMapping("/popular")
    public Flux<StudyMaterialResponse> getPopularStudyMaterials(
            @RequestParam(defaultValue = "10") int limit) {

        return studyMaterialService.getPopularStudyMaterials(limit)
                .map(this::toStudyMaterialResponse);
    }

    @GetMapping("/recent")
    public Flux<StudyMaterialResponse> getRecentStudyMaterials(
            @RequestParam(defaultValue = "10") int limit) {

        return studyMaterialService.getRecentStudyMaterials(limit)
                .map(this::toStudyMaterialResponse);
    }

    private StudyMaterialResponse toStudyMaterialResponse(StudyMaterial material) {
        return new StudyMaterialResponse(
                material.getId() != null ? material.getId().value() : null,
                material.getTitle(),
                material.getDescription(),
                material.getSubject(),
                material.getDifficulty(),
                material.getFileUrl(),
                material.getLikeCount(),
                material.getDownloadCount(),
                material.getAuthorId().value(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }
}