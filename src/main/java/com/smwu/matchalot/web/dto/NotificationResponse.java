package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.Notification;
import com.smwu.matchalot.domain.model.entity.Notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        boolean isRead,
        LocalDateTime createdAt,
        String relatedEntityId
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId() != null ? notification.getId().value() : null,
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getRelatedEntityId()
        );
    }
}