package com.smwu.matchalot;

import com.smwu.matchalot.application.event.MatchEvent;
import com.smwu.matchalot.application.listener.MatchAnalyticsListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class MatchAnalyticsTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private MatchAnalyticsListener analyticsListener;
    
    @BeforeEach
    void setUp() {
        // 테스트 전 통계 초기화
        analyticsListener.resetDailyStatistics();
    }
    
    @Test
    void 매칭_요청_이벤트가_통계에_기록되는지_테스트() throws InterruptedException {
        // given
        String userId = "123";
        Map<String, Object> eventData = Map.of(
            "matchId", 1L,
            "receiverId", 456L,
            "requesterMaterialId", 789L
        );
        
        // when
        MatchEvent event = new MatchEvent(this, userId, "MATCH_REQUESTED", eventData);
        eventPublisher.publishEvent(event);
        
        // 비동기 처리를 위한 대기
        TimeUnit.MILLISECONDS.sleep(100);
        
        // then
        Map<String, Long> stats = analyticsListener.getCurrentStatistics();
        assertThat(stats).isNotEmpty();
        
        // 통계에 매칭 요청이 기록되었는지 확인
        boolean hasMatchRequest = stats.keySet().stream()
            .anyMatch(key -> key.contains("MATCH_REQUESTED"));
        assertThat(hasMatchRequest).isTrue();
        
        System.out.println("📊 현재 통계:");
        stats.forEach((key, value) -> 
            System.out.println("  " + key + ": " + value)
        );
    }
    
    @Test
    void 여러_이벤트_타입이_각각_기록되는지_테스트() throws InterruptedException {
        // given
        String userId = "123";
        
        // when - 여러 타입의 이벤트 발생
        eventPublisher.publishEvent(new MatchEvent(
            this, userId, "MATCH_REQUESTED", Map.of("matchId", 1L)
        ));
        
        eventPublisher.publishEvent(new MatchEvent(
            this, userId, "MATCH_ACCEPTED", Map.of("matchId", 1L)
        ));
        
        eventPublisher.publishEvent(new MatchEvent(
            this, userId, "MATCH_COMPLETED", Map.of("matchId", 1L, "trustScoreUpdated", true)
        ));
        
        // 비동기 처리를 위한 대기
        TimeUnit.MILLISECONDS.sleep(200);
        
        // then
        Map<String, Long> stats = analyticsListener.getCurrentStatistics();
        
        long requestCount = stats.entrySet().stream()
            .filter(e -> e.getKey().contains("MATCH_REQUESTED"))
            .mapToLong(Map.Entry::getValue)
            .sum();
        
        long acceptedCount = stats.entrySet().stream()
            .filter(e -> e.getKey().contains("MATCH_ACCEPTED"))
            .mapToLong(Map.Entry::getValue)
            .sum();
        
        long completedCount = stats.entrySet().stream()
            .filter(e -> e.getKey().contains("total_completed"))
            .mapToLong(Map.Entry::getValue)
            .sum();
        
        assertThat(requestCount).isGreaterThan(0);
        assertThat(acceptedCount).isGreaterThan(0);
        assertThat(completedCount).isGreaterThan(0);
        
        System.out.println("📊 이벤트별 통계:");
        System.out.println("  매칭 요청: " + requestCount);
        System.out.println("  매칭 수락: " + acceptedCount);
        System.out.println("  매칭 완료: " + completedCount);
    }
    
    @Test
    void 사용자별_활동_통계가_기록되는지_테스트() throws InterruptedException {
        // given
        String user1 = "100";
        String user2 = "200";
        
        // when - 서로 다른 사용자가 매칭 요청
        for (int i = 0; i < 5; i++) {
            eventPublisher.publishEvent(new MatchEvent(
                this, user1, "MATCH_REQUESTED", Map.of("matchId", i)
            ));
        }
        
        for (int i = 0; i < 3; i++) {
            eventPublisher.publishEvent(new MatchEvent(
                this, user2, "MATCH_REQUESTED", Map.of("matchId", i + 10)
            ));
        }
        
        // 비동기 처리를 위한 대기
        TimeUnit.MILLISECONDS.sleep(200);
        
        // then
        Map<String, Long> stats = analyticsListener.getCurrentStatistics();
        
        Long user1Activity = stats.get("user_" + user1 + "_MATCH_REQUESTED");
        Long user2Activity = stats.get("user_" + user2 + "_MATCH_REQUESTED");
        
        assertThat(user1Activity).isEqualTo(5L);
        assertThat(user2Activity).isEqualTo(3L);
        
        System.out.println("📊 사용자별 활동:");
        System.out.println("  User " + user1 + ": " + user1Activity + "회");
        System.out.println("  User " + user2 + ": " + user2Activity + "회");
    }
}