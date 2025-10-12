package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.ReportService;
import com.smwu.matchalot.application.service.StudyMaterialService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.web.dto.AdminReportResponse;  // ✅ 추가된 import
import com.smwu.matchalot.web.dto.StudyMaterialResponse;
import com.smwu.matchalot.web.dto.StudyMaterialSummaryResponse;
import com.smwu.matchalot.web.dto.UserResponse;
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
    private final ReportService reportService;

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
     * 관리자가 승인 대기 중인 족보 상세 조회 (전체 내용 포함)
     */
    @GetMapping("/materials/{materialId}")
    public Mono<ResponseEntity<StudyMaterialResponse>> getPendingMaterialDetail(
            @PathVariable Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        StudyMaterialId id = StudyMaterialId.of(materialId);

        return checkAdminPermission(oauth2User)
                .then(studyMaterialService.getStudyMaterial(id))
                .flatMap(material -> {
                    // 승인 대기 중인 자료만 조회 가능
                    if (!material.isPendingApproval()) {
                        return Mono.error(new IllegalArgumentException("승인 대기 중인 자료가 아닙니다"));
                    }
                    return toFullResponse(material);
                })
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build())
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/users/{userId}")
    public Mono<ResponseEntity<Map<String, String>>> forceDeleteUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String reason = request.getOrDefault("reason", "관리자 처리");

        return checkAdminPermission(oauth2User)
                .then(userService.forceDeleteUser(UserId.of(userId), reason))
                .then(Mono.just(ResponseEntity.ok(Map.of(
                        "success", "true",
                        "message", "사용자가 강제 탈퇴 처리되었습니다",
                        "reason", reason
                ))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of(
                                "success", "false",
                                "message", "사용자를 찾을 수 없습니다"
                        )))
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.badRequest().body(Map.of(
                                "success", "false",
                                "message", "관리자는 강제 탈퇴시킬 수 없습니다"
                        )));
    }

    /**
     * 관리자용: 전체 사용자 목록 조회
     */
    @GetMapping("/users")
    public Flux<UserResponse> getAllUsers(
            @RequestParam(required = false) String role,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        return checkAdminPermission(oauth2User)
                .thenMany(userService.getAll(
                        role != null ? UserRole.valueOf(role) : null))
                .map(UserResponse::from);
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

    /**
     * 관리자용: 모든 신고 조회
     */
    @GetMapping("/reports")
    public Flux<AdminReportResponse> getAllReports(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        return checkAdminPermission(oauth2User)
                .thenMany(reportService.getAllReports(
                        status != null ? ReportStatus.valueOf(status) : null))
                .flatMap(this::toAdminReportResponse);
    }

    /**
     * 신고 해결 처리
     */
    @PostMapping("/reports/{reportId}/resolve")
    public Mono<ResponseEntity<Map<String, String>>> resolveReport(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        ReportId id = ReportId.of(reportId);
        String adminNote = request.getOrDefault("note", "처리 완료");

        return checkAdminPermission(oauth2User)
                .then(reportService.resolveReport(id, adminNote))
                .then(Mono.just(ResponseEntity.ok(Map.of(
                        "success", "true",
                        "message", "신고가 해결 처리되었습니다"
                ))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of(
                                "success", "false",
                                "message", "신고를 찾을 수 없습니다"
                        )));
    }

    /**
     * 신고 기각 처리
     */
    @PostMapping("/reports/{reportId}/reject")
    public Mono<ResponseEntity<Map<String, String>>> rejectReport(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        ReportId id = ReportId.of(reportId);
        String adminNote = request.getOrDefault("note", "신고 기각");

        return checkAdminPermission(oauth2User)
                .then(reportService.rejectReport(id, adminNote))
                .then(Mono.just(ResponseEntity.ok(Map.of(
                        "success", "true",
                        "message", "신고가 기각 처리되었습니다"
                ))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of(
                                "success", "false",
                                "message", "신고를 찾을 수 없습니다"
                        )));
    }

    // ==========================================================================
    // Helper Methods
    // ==========================================================================

    /**
     * StudyMaterial을 SummaryResponse로 변환
     */
    private Mono<StudyMaterialSummaryResponse> toSummaryResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialSummaryResponse.from(
                        studyMaterial,
                        uploader.getNickname(),
                        uploader.getTrustScore().value()
                ))
                .switchIfEmpty(Mono.just(StudyMaterialSummaryResponse.from(studyMaterial, "알 수 없음", 0)));
    }

    /**
     * StudyMaterial을 Full Response로 변환 (관리자용 - 모든 내용 포함)
     */
    private Mono<StudyMaterialResponse> toFullResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())
                .map(uploader -> StudyMaterialResponse.from(
                        studyMaterial,
                        uploader.getNickname(),
                        uploader.getTrustScore().value()
                ))
                .switchIfEmpty(Mono.just(StudyMaterialResponse.from(studyMaterial, "알 수 없음", 0)));
    }

    /**
     * ✅ 누락되었던 toAdminReportResponse 메서드
     */
    private Mono<AdminReportResponse> toAdminReportResponse(
            com.smwu.matchalot.domain.model.entity.Report report) {

        // 신고자 닉네임 조회
        Mono<String> reporterNickname = userService.getUserById(report.getReporterId())
                .map(user -> user.getNickname())
                .defaultIfEmpty("알 수 없음");

        // 신고당한 사용자 닉네임 조회
        Mono<String> reportedUserNickname = userService.getUserById(report.getReportedUserId())
                .map(user -> user.getNickname())
                .defaultIfEmpty("알 수 없음");

        // 자료 제목 조회 (자료 신고인 경우)
        Mono<String> materialTitle = report.getMaterialId() != null ?
                studyMaterialService.getStudyMaterial(report.getMaterialId())
                        .map(material -> material.getTitle())
                        .defaultIfEmpty("삭제된 자료") :
                Mono.just(null);

        return Mono.zip(reporterNickname, reportedUserNickname, materialTitle)
                .map(tuple -> AdminReportResponse.from(
                        report,
                        tuple.getT1(),  // reporterNickname
                        tuple.getT2(),  // reportedUserNickname
                        tuple.getT3()   // materialTitle
                ));
    }
}