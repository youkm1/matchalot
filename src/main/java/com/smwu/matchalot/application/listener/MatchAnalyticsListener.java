package com.smwu.matchalot.application.listener;

import com.smwu.matchalot.application.event.MatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 매칭 관련 통계 및 분석 데이터 수집
 * 이벤트 드리븐 방식으로 매칭 활동을 모니터링
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchAnalyticsListener {
    
    // 실시간 통계 저장 (추후 Redis로 변경 가능)
    private final Map<String, AtomicLong> dailyStats = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> hourlyStats = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> userActivityStats = new ConcurrentHashMap<>();
    
    @EventListener
    @Async
    public void collectMatchStatistics(MatchEvent event) {
        try {
            String eventType = event.getEventType();
            String userId = event.getUserId();
            LocalDateTime now = LocalDateTime.now();
            
            // 일별 통계
            String dailyKey = String.format("%s_%s_%s", 
                eventType, 
                now.format(DateTimeFormatter.ISO_LOCAL_DATE),
                "daily"
            );
            dailyStats.computeIfAbsent(dailyKey, k -> new AtomicLong(0)).incrementAndGet();
            
            // 시간별 통계
            String hourlyKey = String.format("%s_%s_h%02d", 
                eventType,
                now.format(DateTimeFormatter.ISO_LOCAL_DATE),
                now.getHour()
            );
            hourlyStats.computeIfAbsent(hourlyKey, k -> new AtomicLong(0)).incrementAndGet();
            
            // 사용자별 활동 통계
            String userKey = String.format("user_%s_%s", userId, eventType);
            userActivityStats.computeIfAbsent(userKey, k -> new AtomicLong(0)).incrementAndGet();
            
            // 이벤트별 상세 로깅
            switch (eventType) {
                case "MATCH_REQUESTED":
                    logMatchRequest(event);
                    break;
                case "MATCH_ACCEPTED":
                    logMatchAccepted(event);
                    break;
                case "MATCH_REJECTED":
                    logMatchRejected(event);
                    break;
                case "MATCH_COMPLETED":
                    logMatchCompleted(event);
                    break;
            }
            
            // 주기적으로 통계 요약 로깅 (1000건마다)
            long totalEvents = dailyStats.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            
            if (totalEvents % 1000 == 0) {
                logStatisticsSummary();
            }
            
        } catch (Exception e) {
            log.error("통계 수집 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    private void logMatchRequest(MatchEvent event) {
        Map<String, Object> data = event.getData();
        log.info("📊 [통계] 매칭 요청 - matchId: {}, receiverId: {}", 
            data.get("matchId"), 
            data.get("receiverId")
        );
        
        // 매칭 요청 패턴 분석
        String pattern = String.format("request_pattern_%s", 
            LocalDateTime.now().getDayOfWeek()
        );
        dailyStats.computeIfAbsent(pattern, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    private void logMatchAccepted(MatchEvent event) {
        Map<String, Object> data = event.getData();
        log.info("📊 [통계] 매칭 수락 - matchId: {}", data.get("matchId"));
        
        // 수락률 계산을 위한 카운터
        dailyStats.computeIfAbsent("total_accepted_today", k -> new AtomicLong(0))
            .incrementAndGet();
    }
    
    private void logMatchRejected(MatchEvent event) {
        Map<String, Object> data = event.getData();
        log.info("📊 [통계] 매칭 거절 - matchId: {}", data.get("matchId"));
        
        // 거절률 계산을 위한 카운터
        dailyStats.computeIfAbsent("total_rejected_today", k -> new AtomicLong(0))
            .incrementAndGet();
    }
    
    private void logMatchCompleted(MatchEvent event) {
        Map<String, Object> data = event.getData();
        log.info("📊 [통계] 매칭 완료 - matchId: {}, 신뢰도 업데이트: {}", 
            data.get("matchId"),
            data.get("trustScoreUpdated")
        );
        
        // 성공적인 매칭 완료 카운트
        dailyStats.computeIfAbsent("total_completed_today", k -> new AtomicLong(0))
            .incrementAndGet();
    }
    
    /**
     * 통계 요약 정보 로깅
     */
    private void logStatisticsSummary() {
        log.info("========== 📊 매칭 통계 요약 ==========");
        
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // 오늘의 통계
        long requestsToday = getStatValue("MATCH_REQUESTED_" + today + "_daily");
        long acceptedToday = getStatValue("total_accepted_today");
        long rejectedToday = getStatValue("total_rejected_today");
        long completedToday = getStatValue("total_completed_today");
        
        log.info("오늘 ({}) 통계:", today);
        log.info("  - 매칭 요청: {}건", requestsToday);
        log.info("  - 매칭 수락: {}건", acceptedToday);
        log.info("  - 매칭 거절: {}건", rejectedToday);
        log.info("  - 매칭 완료: {}건", completedToday);
        
        // 수락률 계산
        if (requestsToday > 0) {
            double acceptanceRate = (acceptedToday * 100.0) / requestsToday;
            log.info("  - 수락률: {}%", String.format("%.2f", acceptanceRate));
        }
        
        // 현재 시간대 통계
        int currentHour = LocalDateTime.now().getHour();
        String hourlyKey = String.format("MATCH_REQUESTED_%s_h%02d", today, currentHour);
        long requestsThisHour = getStatValue(hourlyKey);
        log.info("현재 시간대 ({}시) 매칭 요청: {}건", currentHour, requestsThisHour);
        
        // 가장 활발한 사용자
        userActivityStats.entrySet().stream()
            .filter(e -> e.getKey().contains("MATCH_REQUESTED"))
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(5)
            .forEach(entry -> {
                String userId = entry.getKey().split("_")[1];
                log.info(" 활발한 사용자 - userId: {}, 요청 수: {}",
                    userId, entry.getValue().get());
            });
        
        log.info("=====================================");
    }
    
    private long getStatValue(String key) {
        AtomicLong value = dailyStats.get(key);
        return value != null ? value.get() : 0;
    }
    
    /**
     * 통계 데이터 조회 (관리자 대시보드용)
     */
    public Map<String, Long> getCurrentStatistics() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        
        dailyStats.forEach((key, value) -> 
            stats.put(key, value.get())
        );
        
        hourlyStats.forEach((key, value) -> 
            stats.put(key, value.get())
        );
        
        return stats;
    }
    
    /**
     * 일일 통계 초기화 (스케줄러로 자정에 실행 가능)
     */
    public void resetDailyStatistics() {
        log.info("일일 통계 초기화");
        dailyStats.clear();
        hourlyStats.clear();
    }
}