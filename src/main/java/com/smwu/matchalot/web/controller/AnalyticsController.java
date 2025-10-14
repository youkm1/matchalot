package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.listener.MatchAnalyticsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final MatchAnalyticsListener analyticsListener;

    /**
     * 현재 통계 조회 (관리자 전용)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentStatistics() {
        log.info("📊 통계 조회 요청");
        
        Map<String, Long> rawStats = analyticsListener.getCurrentStatistics();
        Map<String, Object> response = new HashMap<>();
        
        // 오늘 날짜
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // 오늘의 통계 필터링
        Map<String, Long> todayStats = rawStats.entrySet().stream()
            .filter(e -> e.getKey().contains(today))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 전체 통계
        long totalRequests = getStatValue(rawStats, "MATCH_REQUESTED_" + today + "_daily");
        long totalAccepted = getStatValue(rawStats, "total_accepted_today");
        long totalRejected = getStatValue(rawStats, "total_rejected_today");
        long totalCompleted = getStatValue(rawStats, "total_completed_today");
        
        // 시간별 통계
        Map<Integer, Long> hourlyRequests = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            String key = String.format("MATCH_REQUESTED_%s_h%02d", today, hour);
            hourlyRequests.put(hour, getStatValue(rawStats, key));
        }
        
        // 응답 구성
        response.put("summary", Map.of(
            "date", today,
            "totalRequests", totalRequests,
            "totalAccepted", totalAccepted,
            "totalRejected", totalRejected,
            "totalCompleted", totalCompleted,
            "acceptanceRate", totalRequests > 0 ? 
                String.format("%.2f%%", (totalAccepted * 100.0) / totalRequests) : "0%"
        ));
        
        response.put("hourlyRequests", hourlyRequests);
        response.put("rawStatistics", todayStats);
        
        log.info("📊 통계 조회 완료 - 오늘 요청: {}건", totalRequests);
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    /**
     * 시간대별 통계 조회
     */
    @GetMapping("/statistics/hourly")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> getHourlyStatistics(
            @RequestParam(required = false) String date) {
        
        String targetDate = date != null ? date : 
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        log.info("📊 시간대별 통계 조회 - 날짜: {}", targetDate);
        
        Map<String, Long> rawStats = analyticsListener.getCurrentStatistics();
        Map<String, Object> response = new HashMap<>();
        
        // 각 이벤트 타입별 시간대 통계
        Map<String, Map<Integer, Long>> eventTypeHourlyStats = new HashMap<>();
        String[] eventTypes = {"MATCH_REQUESTED", "MATCH_ACCEPTED", "MATCH_REJECTED", "MATCH_COMPLETED"};
        
        for (String eventType : eventTypes) {
            Map<Integer, Long> hourlyData = new HashMap<>();
            for (int hour = 0; hour < 24; hour++) {
                String key = String.format("%s_%s_h%02d", eventType, targetDate, hour);
                hourlyData.put(hour, getStatValue(rawStats, key));
            }
            eventTypeHourlyStats.put(eventType, hourlyData);
        }
        
        // 피크 시간 계산
        int peakHour = -1;
        long peakCount = 0;
        Map<Integer, Long> requestHourly = eventTypeHourlyStats.get("MATCH_REQUESTED");
        for (Map.Entry<Integer, Long> entry : requestHourly.entrySet()) {
            if (entry.getValue() > peakCount) {
                peakCount = entry.getValue();
                peakHour = entry.getKey();
            }
        }
        
        response.put("date", targetDate);
        response.put("hourlyStatsByEventType", eventTypeHourlyStats);
        response.put("peakHour", peakHour);
        response.put("peakCount", peakCount);
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    /**
     * 사용자별 활동 통계
     */
    @GetMapping("/statistics/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> getUserStatistics(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("📊 사용자별 활동 통계 조회 - 상위 {}명", limit);
        
        Map<String, Long> rawStats = analyticsListener.getCurrentStatistics();
        Map<String, Object> response = new HashMap<>();
        
        // 사용자별 통계 집계
        Map<String, Map<String, Long>> userStats = new HashMap<>();
        
        rawStats.entrySet().stream()
            .filter(e -> e.getKey().startsWith("user_"))
            .forEach(entry -> {
                String[] parts = entry.getKey().split("_");
                if (parts.length >= 3) {
                    String userId = parts[1];
                    String eventType = parts[2];
                    
                    userStats.computeIfAbsent(userId, k -> new HashMap<>())
                        .put(eventType, entry.getValue());
                }
            });
        
        // 가장 활발한 사용자 정렬
        Map<String, Map<String, Long>> topUsers = userStats.entrySet().stream()
            .sorted((e1, e2) -> {
                long total1 = e1.getValue().values().stream().mapToLong(Long::longValue).sum();
                long total2 = e2.getValue().values().stream().mapToLong(Long::longValue).sum();
                return Long.compare(total2, total1);
            })
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                HashMap::new
            ));
        
        response.put("topActiveUsers", topUsers);
        response.put("totalUsers", userStats.size());
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    /**
     * 매칭 성공률 통계
     */
    @GetMapping("/statistics/success-rate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> getSuccessRateStatistics() {
        log.info("📊 매칭 성공률 통계 조회");
        
        Map<String, Long> rawStats = analyticsListener.getCurrentStatistics();
        Map<String, Object> response = new HashMap<>();
        
        // 최근 7일간의 성공률 계산
        Map<String, Double> dailySuccessRates = new HashMap<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            long requests = getStatValue(rawStats, "MATCH_REQUESTED_" + dateStr + "_daily");
            long completed = getStatValue(rawStats, "total_completed_" + dateStr);
            
            double successRate = requests > 0 ? (completed * 100.0) / requests : 0;
            dailySuccessRates.put(dateStr, successRate);
        }
        
        // 요일별 패턴 분석
        Map<String, Long> weekdayPatterns = new HashMap<>();
        rawStats.entrySet().stream()
            .filter(e -> e.getKey().startsWith("request_pattern_"))
            .forEach(entry -> {
                String dayOfWeek = entry.getKey().replace("request_pattern_", "");
                weekdayPatterns.put(dayOfWeek, entry.getValue());
            });
        
        response.put("dailySuccessRates", dailySuccessRates);
        response.put("weekdayPatterns", weekdayPatterns);
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    /**
     * 통계 초기화 (관리자 전용)
     */
    @PostMapping("/statistics/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> resetStatistics() {
        log.warn("📊 통계 초기화 요청");
        
        analyticsListener.resetDailyStatistics();
        
        Map<String, String> response = Map.of(
            "message", "통계가 초기화되었습니다",
            "timestamp", LocalDateTime.now().toString()
        );
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    private long getStatValue(Map<String, Long> stats, String key) {
        return stats.getOrDefault(key, 0L);
    }
}