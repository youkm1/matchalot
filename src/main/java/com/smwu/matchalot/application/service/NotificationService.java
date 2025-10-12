package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.Notification;
import com.smwu.matchalot.domain.model.entity.Notification.NotificationType;
import com.smwu.matchalot.domain.model.vo.NotificationId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.reposiotry.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamService streamService;
    private final EmailService emailService;
    private final UserService userService;

    public Mono<Notification> createNotification(UserId userId, NotificationType type, 
                                                 String title, String message, String relatedEntityId) {
        Notification notification = new Notification(userId, type, title, message, relatedEntityId);
        return notificationRepository.save(notification)
                .flatMap(saved -> {
                    log.info("알림 생성: 사용자={}, 타입={}, 제목={}", userId.value(), type, title);
                    
                    // 실시간 알림 전송 (SSE)
                    streamService.emit(userId, saved);
                    
                    // 이메일 전송 (비동기)
                    return userService.getUserById(userId)
                            .flatMap(user -> emailService.sendNotificationEmail(
                                    user.getEmail(), 
                                    type, 
                                    title, 
                                    message
                            ))
                            .then(Mono.just(saved))
                            .onErrorResume(e -> {
                                log.warn("이메일 전송 실패, SSE만 전송: {}", e.getMessage());
                                return Mono.just(saved);
                            });
                });
    }

    public Flux<Notification> getUserNotifications(UserId userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Flux<Notification> getUnreadNotifications(UserId userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    public Mono<Long> getUnreadCount(UserId userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public Mono<Notification> markAsRead(NotificationId id) {
        return notificationRepository.findById(id)
                .flatMap(notification -> {
                    Notification readNotification = notification.markAsRead();
                    return notificationRepository.save(readNotification);
                });
    }

    public Mono<Void> markAllAsRead(UserId userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    public Mono<Void> deleteNotification(NotificationId id, UserId userId) {
        return notificationRepository.findById(id)
                .filter(notification -> notification.getUserId().equals(userId))
                .flatMap(notification -> notificationRepository.deleteById(id))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("알림을 찾을 수 없거나 권한이 없습니다")));
    }

    public Mono<Notification> notifyUserPromotion(UserId userId, String newRole) {
        String title = "회원 등급 상승";
        String message = String.format("축하합니다! %s로 승격되었습니다.", newRole);
        return createNotification(userId, NotificationType.USER_PROMOTED, title, message, userId.toString());
    }

    public Mono<Notification> notifyMaterialApproval(UserId userId, String materialTitle, Long materialId) {
        String title = "족보 승인 완료";
        String message = String.format("업로드하신 '%s' 족보가 승인되었습니다.", materialTitle);
        return createNotification(userId, NotificationType.MATERIAL_APPROVED, title, message, materialId.toString());
    }

    public Mono<Notification> notifyMaterialRejection(UserId userId, String materialTitle, String reason, Long materialId) {
        String title = "족보 승인 거절";
        String message = String.format("업로드하신 '%s' 족보가 거절되었습니다. 사유: %s", materialTitle, reason);
        return createNotification(userId, NotificationType.MATERIAL_REJECTED, title, message, materialId.toString());
    }

    public Mono<Notification> notifyMatchCompleted(UserId userId, String partnerNickname, String materialTitle, Long matchId) {
        String title = "매칭 성사";
        String message = String.format("%s님과 매칭이 완료되었습니다. '%s' 족보를 확인하실 수 있습니다.", 
            partnerNickname, materialTitle);
        return createNotification(userId, NotificationType.MATCH_COMPLETED, title, message, matchId.toString());
    }

    public Mono<Notification> notifyMatchRequestReceived(UserId userId, String requesterNickname, Long matchId) {
        String title = "매칭 요청";
        String message = String.format("%s님이 매칭을 요청했습니다. 확인해주세요.", requesterNickname);
        return createNotification(userId, NotificationType.MATCH_REQUEST_RECEIVED, title, message, matchId.toString());
    }
}