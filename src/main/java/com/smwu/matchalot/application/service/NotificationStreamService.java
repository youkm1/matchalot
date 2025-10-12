package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.Notification;
import com.smwu.matchalot.domain.model.vo.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationStreamService {

    // 사용자별 Sink 관리 (여러 연결 허용)
    private final Map<Long, Sinks.Many<Notification>> userSinks = new ConcurrentHashMap<>();

    /**
     * 사용자의 실시간 알림 스트림 구독
     */
    public Flux<Notification> subscribe(UserId userId) {
        log.info("사용자 {} 알림 스트림 구독 시작", userId.value());
        
        // 사용자별 Sink 생성 또는 가져오기
        Sinks.Many<Notification> sink = userSinks.computeIfAbsent(
            userId.value(),
            id -> Sinks.many().multicast().onBackpressureBuffer()
        );
        
        return sink.asFlux()
            .doOnSubscribe(sub -> log.info("사용자 {} 알림 스트림 연결됨", userId.value()))
            .doOnCancel(() -> {
                log.info("사용자 {} 알림 스트림 연결 해제", userId.value());
                // 연결이 끊어져도 Sink는 유지 (다른 연결이 있을 수 있음)
            })
            .doOnError(error -> log.error("사용자 {} 알림 스트림 오류: {}", userId.value(), error.getMessage()));
    }

    /**
     * 특정 사용자에게 실시간 알림 전송
     */
    public void emit(UserId userId, Notification notification) {
        Sinks.Many<Notification> sink = userSinks.get(userId.value());
        
        if (sink != null) {
            Sinks.EmitResult result = sink.tryEmitNext(notification);
            
            if (result.isSuccess()) {
                log.debug("사용자 {}에게 실시간 알림 전송 성공: {}", userId.value(), notification.getTitle());
            } else {
                log.warn("사용자 {}에게 실시간 알림 전송 실패: {}", userId.value(), result);
            }
        } else {
            log.debug("사용자 {}의 활성 스트림이 없음 (오프라인)", userId.value());
        }
    }

    /**
     * 사용자 연결 정리 (로그아웃 시 호출)
     */
    public void cleanup(UserId userId) {
        Sinks.Many<Notification> sink = userSinks.remove(userId.value());
        if (sink != null) {
            sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
            log.info("사용자 {} 알림 스트림 정리 완료", userId.value());
        }
    }

    /**
     * 현재 연결된 사용자 수 조회
     */
    public int getConnectedUsersCount() {
        return userSinks.size();
    }

    /**
     * 특정 사용자가 연결되어 있는지 확인
     */
    public boolean isUserConnected(UserId userId) {
        return userSinks.containsKey(userId.value());
    }
}