package com.smwu.matchalot.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smwu.matchalot.application.event.MatchEvent;
import org.springframework.context.event.EventListener;
import com.smwu.matchalot.domain.model.vo.MatchId;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Sinks.Many<MatchNotification> notificationSink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("WebSocket 연결 실패: userId가 없음");
            return session.close();
        }

        sessions.put(userId, session);
        log.info("사용자 {} WebSocket 연결됨", userId);

        // 클라이언트 메시지 처리
        Mono<Void> input = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(message -> handleIncomingMessage(userId, message))
            .then();

        // 실시간 알림 스트림 구독
        Mono<Void> output = session.send(
            notificationSink.asFlux()
                .filter(notification -> userId.equals(notification.getUserId()))
                .map(notification -> {
                    try {
                        String json = objectMapper.writeValueAsString(notification);
                        return session.textMessage(json);
                    } catch (Exception e) {
                        log.error("JSON 변환 실패", e);
                        return session.textMessage("{\"error\":\"message_conversion_failed\"}");
                    }
                })
        );

        // 양방향 통신 처리
        return Mono.zip(input, output)
            .then()
            .doFinally(signalType -> {
                sessions.remove(userId);
                log.info("사용자 {} WebSocket 연결 종료", userId);
            });
    }

    @EventListener
    public void handleMatchEvent(MatchEvent event) {
        log.info("매치 이벤트 수신: userId={}, type={}", event.getUserId(), event.getEventType());
        sendMatchNotification(event.getUserId(), event.getEventType(), event.getData());
    }

    private Mono<Void> handleIncomingMessage(String userId, String messageJson) {
        log.info("WebSocket 메시지 수신: userId={}, message={}", userId, messageJson);
        // 단순히 연결 상태 확인용 ping/pong 처리
        if (messageJson.contains("ping")) {
            sendMatchNotification(userId, "pong", Map.of("timestamp", System.currentTimeMillis()));
        }
        return Mono.empty();
    }

    public void sendMatchNotification(String userId, String type, Object data) {
        MatchNotification notification = new MatchNotification(userId, type, data);
        notificationSink.tryEmitNext(notification);
        log.info("매칭 알림 전송: userId={}, type={}", userId, type);
    }

    private Mono<Void> sendErrorToUser(String userId, String errorMessage) {
        MatchNotification errorNotification = new MatchNotification(
            userId, 
            "ERROR", 
            Map.of("message", errorMessage)
        );
        notificationSink.tryEmitNext(errorNotification);
        return Mono.empty();
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // JWT 토큰에서 사용자 ID 추출 로직
        // 현재는 쿼리 파라미터로 받는다고 가정
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.replace("userId=", "");
        }
        return null;
    }

    public static class WebSocketMessageDto {
        private String type;
        private Object data;

        public WebSocketMessageDto() {}
        public WebSocketMessageDto(String type, Object data) {
            this.type = type;
            this.data = data;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    public static class MatchNotification {
        private String userId;
        private String type;
        private Object data;

        public MatchNotification(String userId, String type, Object data) {
            this.userId = userId;
            this.type = type;
            this.data = data;
        }

        public String getUserId() { return userId; }
        public String getType() { return type; }
        public Object getData() { return data; }
    }
}
