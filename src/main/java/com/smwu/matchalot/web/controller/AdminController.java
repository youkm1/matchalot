package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.StudyMaterialService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.web.dto.StudyMaterialSummaryResponse;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StudyMaterialService studyMaterialService;
    private final UserService userService;

    /**
     * 관리자 권한 확인 미들웨어
     */
    private Mono<Void> checkAdminPermission(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.isAdminByEmail(userEmail)
                .flatMap(isAdmin -> {
                    if (!isAdmin) {
                        return Mono.error(new IllegalStateException("관리자 권한이 필요합니다."));
                    }
                    return Mono.empty();
                });
    }

    /**
     * 승인 대기 중인 족보 목록 조회
     */
    @GetMapping("/materials/pending")
    public Flux<StudyMaterialSummaryResponse> getPendingMaterials(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        return checkAdminPermission(oauth2User)
                .thenMany(studyMaterialService.getPendingMaterials())
                .flatMap(this::toSummaryResponse);
    }

    /**
     * 족보 승인
     */
    @PutMapping("/materials/{materialId}/approve")
    public Mono<ResponseEntity<Map<String, String>>> approveMaterial(
            @PathVariable Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        StudyMaterialId id = StudyMaterialId.of(materialId);

        return checkAdminPermission(oauth2User)
                .then(studyMaterialService.approveMaterial(id))
                .then(Mono.just(ResponseEntity.ok(Map.of(
                        "message", "족보가 승인되었습니다.",
                        "materialId", materialId.toString()
                ))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of("error", "족보를 찾을 수 없습니다.")))
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "권한이 없거나 승인할 수 없는 상태입니다.")));
    }

    /**
     * 족보 거절
     */
    @PutMapping("/materials/{materialId}/reject")
    public Mono<ResponseEntity<Map<String, String>>> rejectMaterial(
            @PathVariable Long materialId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        StudyMaterialId id = StudyMaterialId.of(materialId);
        String reason = body != null ? body.get("reason") : "승인 기준에 부합하지 않습니다.";

        return checkAdminPermission(oauth2User)
                .then(studyMaterialService.rejectMaterial(id, reason))
                .then(Mono.just(ResponseEntity.ok(Map.of(
                        "message", "족보가 거절되었습니다.",
                        "materialId", materialId.toString(),
                        "reason", reason
                ))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of("error", "족보를 찾을 수 없습니다.")))
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "권한이 없거나 거절할 수 없는 상태입니다.")));
    }

    /**
     * 관리자 대시보드 통계
     */
    @GetMapping("/dashboard")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        return checkAdminPermission(oauth2User)
                .then(studyMaterialService.getAdminStatistics())
                .map(stats -> ResponseEntity.ok(Map.of(
                        "message", "관리자 대시보드 정보",
                        "statistics", stats
                )))
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "관리자 권한이 필요합니다.")));
    }

    // Helper method for response conversion
    private Mono<StudyMaterialSummaryResponse> toSummaryResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialSummaryResponse.from(
                        studyMaterial,
                        uploader.getNickname(),
                        uploader.getTrustScore().value()
                ))
                .switchIfEmpty(Mono.just(StudyMaterialSummaryResponse.from(studyMaterial, "알 수 없음", 0)));
    }
}