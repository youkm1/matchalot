package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.NotificationService;
import com.smwu.matchalot.application.service.NotificationStreamService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.NotificationId;
import com.smwu.matchalot.web.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final NotificationStreamService streamService;

    @GetMapping
    public Flux<NotificationResponse> getMyNotifications(
            @Parameter(description = "읽지 않은 알림만 조회")
            @RequestParam(value = "unread", required = false) Boolean unreadOnly,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Flux.empty();
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> {
                    if (Boolean.TRUE.equals(unreadOnly)) {
                        return notificationService.getUnreadNotifications(user.getId());
                    }
                    return notificationService.getUserNotifications(user.getId());
                })
                .map(NotificationResponse::from);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "읽지 않은 알림의 개수를 반환합니다")
    public Mono<ResponseEntity<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> notificationService.getUnreadCount(user.getId()))
                .map(count -> ResponseEntity.ok(Map.of("unreadCount", count)))
                .defaultIfEmpty(ResponseEntity.ok(Map.of("unreadCount", 0L)));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다")
    public Mono<ResponseEntity<NotificationResponse>> markAsRead(
            @Parameter(description = "알림 ID")
            @PathVariable Long notificationId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        NotificationId id = NotificationId.of(notificationId);

        return notificationService.markAsRead(id)
                .map(notification -> ResponseEntity.ok(NotificationResponse.from(notification)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다")
    public Mono<ResponseEntity<Map<String, String>>> markAllAsRead(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> notificationService.markAllAsRead(user.getId()))
                .then(Mono.just(ResponseEntity.ok(Map.of("message", "모든 알림이 읽음 처리되었습니다"))))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다")
    public Mono<ResponseEntity<Map<String, String>>> deleteNotification(
            @Parameter(description = "알림 ID")
            @PathVariable Long notificationId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        NotificationId id = NotificationId.of(notificationId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> notificationService.deleteNotification(id, user.getId()))
                .then(Mono.just(ResponseEntity.ok(Map.of("message", "알림이 삭제되었습니다"))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of("error", "알림을 찾을 수 없거나 권한이 없습니다")));
    }

    /**
     * SSE (Server-Sent Events)를 통한 실시간 알림 스트림
     * 클라이언트는 이 엔드포인트에 연결하여 실시간 알림을 받을 수 있음
     */
    @GetMapping(value = "/stream", produces = "text/event-stream")
    @Operation(summary = "실시간 알림 스트림", description = "SSE를 통한 실시간 알림을 수신합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스트림 연결 성공",
                    content = @Content(mediaType = "text/event-stream")),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public Flux<ServerSentEvent<NotificationResponse>> streamNotifications(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return Flux.empty();
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> {
                    log.info("SSE 연결 시작: 사용자 {}", user.getId().value());
                    
                    // 실시간 알림 스트림 구독
                    return streamService.subscribe(user.getId())
                            .map(notification -> ServerSentEvent.<NotificationResponse>builder()
                                    .id(String.valueOf(notification.getId() != null ? notification.getId().value() : System.currentTimeMillis()))
                                    .event("notification")
                                    .data(NotificationResponse.from(notification))
                                    .build())
                            // 30초마다 heartbeat 전송 (연결 유지)
                            .mergeWith(Flux.interval(Duration.ofSeconds(30))
                                    .map(tick -> ServerSentEvent.<NotificationResponse>builder()
                                            .event("heartbeat")
                                            .comment("keep-alive")
                                            .build()))
                            .doOnCancel(() -> log.info("SSE 연결 종료: 사용자 {}", user.getId().value()))
                            .doOnError(error -> log.error("SSE 오류: 사용자 {}, 오류: {}", user.getId().value(), error.getMessage()));
                })
                .switchIfEmpty(Flux.empty());
    }
}