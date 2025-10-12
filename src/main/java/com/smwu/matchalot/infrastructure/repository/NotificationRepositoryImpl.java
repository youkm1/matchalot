package com.smwu.matchalot.infrastructure.repository;

import com.smwu.matchalot.domain.model.entity.Notification;
import com.smwu.matchalot.domain.model.entity.Notification.NotificationType;
import com.smwu.matchalot.domain.model.vo.NotificationId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.reposiotry.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NotificationRepositoryImpl implements NotificationRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Notification> save(Notification notification) {
        if (notification.getId() == null) {
            return insert(notification);
        }
        return update(notification);
    }

    private Mono<Notification> insert(Notification notification) {
        String sql = """
            INSERT INTO notifications (user_id, type, title, message, is_read, created_at, related_entity_id)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            RETURNING id, user_id, type, title, message, is_read, created_at, related_entity_id
            """;

        return databaseClient.sql(sql)
                .bind("$1", notification.getUserId().value())
                .bind("$2", notification.getType().name())
                .bind("$3", notification.getTitle())
                .bind("$4", notification.getMessage())
                .bind("$5", notification.isRead())
                .bind("$6", notification.getCreatedAt())
                .bind("$7", notification.getRelatedEntityId())
                .map(this::mapToNotification)
                .one()
                .doOnSuccess(saved -> log.info("알림 생성: 사용자={}, 타입={}", 
                    notification.getUserId().value(), notification.getType()));
    }

    private Mono<Notification> update(Notification notification) {
        String sql = """
            UPDATE notifications 
            SET is_read = $1
            WHERE id = $2
            RETURNING id, user_id, type, title, message, is_read, created_at, related_entity_id
            """;

        return databaseClient.sql(sql)
                .bind("$1", notification.isRead())
                .bind("$2", notification.getId().value())
                .map(this::mapToNotification)
                .one();
    }

    @Override
    public Mono<Notification> findById(NotificationId id) {
        String sql = """
            SELECT id, user_id, type, title, message, is_read, created_at, related_entity_id
            FROM notifications
            WHERE id = $1
            """;

        return databaseClient.sql(sql)
                .bind("$1", id.value())
                .map(this::mapToNotification)
                .one();
    }

    @Override
    public Flux<Notification> findByUserId(UserId userId) {
        String sql = """
            SELECT id, user_id, type, title, message, is_read, created_at, related_entity_id
            FROM notifications
            WHERE user_id = $1
            ORDER BY created_at DESC
            """;

        return databaseClient.sql(sql)
                .bind("$1", userId.value())
                .map(this::mapToNotification)
                .all();
    }

    @Override
    public Flux<Notification> findUnreadByUserId(UserId userId) {
        String sql = """
            SELECT id, user_id, type, title, message, is_read, created_at, related_entity_id
            FROM notifications
            WHERE user_id = $1 AND is_read = false
            ORDER BY created_at DESC
            """;

        return databaseClient.sql(sql)
                .bind("$1", userId.value())
                .map(this::mapToNotification)
                .all();
    }

    @Override
    public Mono<Long> countUnreadByUserId(UserId userId) {
        String sql = """
            SELECT COUNT(*)
            FROM notifications
            WHERE user_id = $1 AND is_read = false
            """;

        return databaseClient.sql(sql)
                .bind("$1", userId.value())
                .map(row -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Void> markAsRead(NotificationId id) {
        String sql = """
            UPDATE notifications
            SET is_read = true
            WHERE id = $1
            """;

        return databaseClient.sql(sql)
                .bind("$1", id.value())
                .then();
    }

    @Override
    public Mono<Void> markAllAsReadByUserId(UserId userId) {
        String sql = """
            UPDATE notifications
            SET is_read = true
            WHERE user_id = $1 AND is_read = false
            """;

        return databaseClient.sql(sql)
                .bind("$1", userId.value())
                .then();
    }

    @Override
    public Mono<Void> deleteById(NotificationId id) {
        String sql = "DELETE FROM notifications WHERE id = $1";

        return databaseClient.sql(sql)
                .bind("$1", id.value())
                .then();
    }

    private Notification mapToNotification(io.r2dbc.spi.Row row) {
        return new Notification(
                NotificationId.of(row.get("id", Long.class)),
                UserId.of(row.get("user_id", Long.class)),
                NotificationType.valueOf(row.get("type", String.class)),
                row.get("title", String.class),
                row.get("message", String.class),
                row.get("is_read", Boolean.class),
                row.get("created_at", LocalDateTime.class),
                row.get("related_entity_id", String.class)
        );
    }
}