package com.smwu.matchalot.domain.model.entity;

import com.smwu.matchalot.domain.model.vo.NotificationId;
import com.smwu.matchalot.domain.model.vo.UserId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Notification {
    private final NotificationId id;
    private final UserId userId;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final boolean isRead;
    private final LocalDateTime createdAt;
    private final String relatedEntityId;

    public Notification(UserId userId, NotificationType type, String title, String message, String relatedEntityId) {
        this.id = null;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
        this.relatedEntityId = relatedEntityId;
    }

    public Notification(NotificationId id, UserId userId, NotificationType type, 
                       String title, String message, boolean isRead, 
                       LocalDateTime createdAt, String relatedEntityId) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.relatedEntityId = relatedEntityId;
    }

    public Notification markAsRead() {
        return new Notification(id, userId, type, title, message, true, createdAt, relatedEntityId);
    }

    public enum NotificationType {
        USER_PROMOTED("회원 등급 상승"),
        MATERIAL_APPROVED("족보 승인"),
        MATERIAL_REJECTED("족보 거절"),
        MATCH_COMPLETED("매칭 성사"),
        MATCH_REQUEST_RECEIVED("매칭 요청 받음"),
        SYSTEM("시스템 알림");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}