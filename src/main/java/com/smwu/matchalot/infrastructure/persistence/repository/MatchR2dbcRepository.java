package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.infrastructure.persistence.MatchEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface MatchR2dbcRepository extends R2dbcRepository<MatchEntity, Long> {
    @Query("SELECT * FROM matches WHERE requester_id = :userId OR receiver_id = :userId ORDER BY created_at DESC")
    Flux<MatchEntity> findByUserId(Long userId);
    @Query("SELECT * FROM matches WHERE requester_id = :userI OR receiver_id = :userId ORDER BY created_at DESC")
    Flux<MatchEntity> findMatchesByUser(Long userId);

    @Query("SELECT * FROM matches WHERE (requester_id = :userId OR receiver_id = :userId) AND status = :status ORDER BY created_at DESC")
    Flux<MatchEntity> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT * FROM matches WHERE receiver_id = :receiverId AND status = 'PENDING' ORDER BY created_at DESC")
    Flux<MatchEntity> findPendingRequestsToUser(Long receiverId);

    @Query("SELECT * FROM matches WHERE requester_id = :requesterId AND status = 'PENDING' ORDER BY created_at DESC")
    Flux<MatchEntity> findSentRequestsByUser(Long requesterId);

    @Query("SELECT COUNT(*) > 0 FROM matches WHERE " +
            "((requester_id = :userId1 AND receiver_id = :userId2) OR " +
            " (requester_id = :userId2 AND receiver_id = :userId1)) AND " +
            "status IN ('PENDING', 'ACCEPTED')")
    Mono<Boolean> existsPendingMatchBetween(Long userId1, Long userId2);

    @Query("SELECT COUNT(*) > 0 FROM matches WHERE " +
            "(requester_material_id = :materialId OR receiver_material_id = :materialId) AND " +
            "status IN ('PENDING', 'ACCEPTED')")
    Mono<Boolean> existsActiveMatchForMaterial(Long materialId);

    // 특정 족보 간 매칭
    @Query("SELECT * FROM matches WHERE " +
            "((requester_material_id = :materialId1 AND receiver_material_id = :materialId2) OR " +
            " (requester_material_id = :materialId2 AND receiver_material_id = :materialId1)) " +
            "ORDER BY created_at DESC")
    Flux<MatchEntity> findByMaterialIds(Long materialId1, Long materialId2);

    // 만료된 매칭
    @Query("SELECT * FROM matches WHERE expires_at < :now AND status IN ('PENDING', 'ACCEPTED')")
    Flux<MatchEntity> findExpiredMatches(LocalDateTime now);

    // 통계
    @Query("SELECT COUNT(*) FROM matches WHERE (requester_id = :userId OR receiver_id = :userId) AND status = :status")
    Mono<Long> countByUserIdAndStatus(Long userId, String status);



    @Query("SELECT COUNT(*) FROM matches WHERE requester_id = :userId OR receiver_id = :userId")
    Mono<Long> countTotalMatchesByUserId(Long userId);

    @Query("SELECT COUNT(*) FROM matches WHERE " +
            "(requester_id = :userId AND requester_material_id = :materialId) OR " +
            "(receiver_id = :userId AND receiver_material_id = :materialId)")
    Mono<Long> countByUserIdAndStudyMaterialId(Long userId, Long materialId);

    @Query("SELECT * FROM matches WHERE " +
            "((requester_id = :userId AND requester_material_id = :materialId) OR " +
            "(receiver_id = :userId AND receiver_material_id = :materialId)) AND " +
            "status = 'COMPLETED' " +
            "ORDER BY created_at DESC LIMIT 1")
    Mono<MatchEntity> findCompletedMatchByUserAndMaterial(Long userId, Long materialId);

}
