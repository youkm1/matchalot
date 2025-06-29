package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.ReportService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.web.dto.ReportDetailResponse;
import com.smwu.matchalot.web.dto.ReportRequestDto;
import com.smwu.matchalot.web.dto.ReportSummaryResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> submitReport(
            @Valid @RequestBody ReportRequestDto request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(reporter -> reportService.submitReport(
                        reporter.getId(),
                        UserId.of(request.reportedUserId()),
                        request.materialId() != null ? StudyMaterialId.of(request.materialId()) : null,
                        ReportType.valueOf(request.type()),
                        request.description()
                ))
                .map(report -> {
                    Map<String, Object> responseBody = Map.of(
                            "success", true,
                            "message", "신고가 접수되었습니다",
                            "reportId", report.getId().value()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
                })
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of(
                                "success", (Object) false,  // ✅ 명시적 캐스팅
                                "message", (Object) "잘못된 신고 정보입니다"
                        )))
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                                "success", (Object) false,  // ✅ 명시적 캐스팅
                                "message", (Object) "이미 신고한 사용자/자료입니다"
                        )));
    }

    @GetMapping("/my")
    public Flux<ReportSummaryResponse> getMyReports(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> {
                    if (status != null) {
                        // 상태별 필터링
                        return reportService.getReportsByReporterAndStatus(
                                user.getId(), ReportStatus.valueOf(status));
                    } else {
                        // 전체 조회
                        return reportService.getReportsByReporter(user.getId());  // ✅ 이제 사용 가능
                    }
                })
                .flatMap(this::toSummaryResponse);
    }


    @GetMapping("/{reportId}")
    public Mono<ResponseEntity<ReportDetailResponse>> getReportDetail(
            @PathVariable Long reportId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        ReportId id = ReportId.of(reportId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> reportService.getReport(id, user.getId()))
                .flatMap(this::toDetailResponse)
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.notFound().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @GetMapping("/check")
    public Mono<ResponseEntity<Map<String, Object>>> checkIfAlreadyReported(
            @RequestParam Long targetUserId,
            @RequestParam(required = false) Long materialId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> {
                    if (materialId != null) {
                        // 자료 신고 여부 확인
                        return reportService.hasReportedMaterial(user.getId(), StudyMaterialId.of(materialId));
                    } else {
                        // 사용자 신고 여부 확인
                        return reportService.hasReportedUser(user.getId(), UserId.of(targetUserId));
                    }
                })
                .map(hasReported -> {
                    Map<String, Object> responseBody = Map.of("alreadyReported", hasReported);
                    return ResponseEntity.ok(responseBody);
                })
                .defaultIfEmpty(ResponseEntity.ok(Map.of("alreadyReported", (Object) false)));
    }

    private Mono<ReportSummaryResponse> toSummaryResponse(
            com.smwu.matchalot.domain.model.entity.Report report) {
        return userService.getUserById(report.getReportedUserId())
                .map(reportedUser -> ReportSummaryResponse.from(report, reportedUser.getNickname()))
                .switchIfEmpty(Mono.just(ReportSummaryResponse.from(report, "알 수 없음")));
    }

    private Mono<ReportDetailResponse> toDetailResponse(
            com.smwu.matchalot.domain.model.entity.Report report) {
        return userService.getUserById(report.getReportedUserId())
                .map(reportedUser -> ReportDetailResponse.from(report, reportedUser.getNickname()))
                .switchIfEmpty(Mono.just(ReportDetailResponse.from(report, "알 수 없음")));
    }
}
