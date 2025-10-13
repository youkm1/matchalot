package com.smwu.matchalot.domain.repository;

import com.smwu.matchalot.domain.model.entity.Notification;
import com.smwu.matchalot.domain.model.vo.NotificationId;
import com.smwu.matchalot.domain.model.vo.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository {
    Mono<Notification> save(Notification notification);
    Mono<Notification> findById(NotificationId id);
    Flux<Notification> findByUserId(UserId userId);
    Flux<Notification> findUnreadByUserId(UserId userId);
    Mono<Long> countUnreadByUserId(UserId userId);
    Mono<Void> markAsRead(NotificationId id);
    Mono<Void> markAllAsReadByUserId(UserId userId);
    Mono<Void> deleteById(NotificationId id);
}