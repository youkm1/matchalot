package com.smwu.matchalot.domain.repository;

import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.vo.MatchId;
import com.smwu.matchalot.domain.model.vo.MatchStatus;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MatchRepository {
    Mono<Match> save(Match match);
    Mono<Match> findById(MatchId matchId);
    Mono<Void> deleteById(MatchId matchId);

    Flux<Match> findByUserId(UserId userId);
    Flux<Match> findPendingMatchesByUserId(UserId userId);
    Flux<Match> findAcceptedMatchesByUserId(UserId userId);
    Flux<Match> findCompletedMatchesByUserId(UserId userId);

    Flux<Match> findPendingRequestsToUser(UserId receiverId);
    Flux<Match> findSentRequestsByUser(UserId myId);
    
    // MatchService에서 사용하는 메서드 추가
    Flux<Match> findByReceiverId(UserId receiverId);
    Flux<Match> findByRequesterId(UserId requesterId);
    Flux<Match> findByUserIdInvolved(UserId userId);
    Mono<Match> findCompletedMatchByUserAndMaterial(UserId userId, StudyMaterialId materialId);

    Mono<Boolean> existsPendingMatchBetween(UserId user1, UserId user2);
    Mono<Boolean> existsActiveMAtchForMaterial(StudyMaterialId materialId);

    Flux<Match> findByMaterialIds(StudyMaterialId materialId1, StudyMaterialId materialId2);

    Flux<Match> findExpiredMatches();

    Mono<Long> countByUserIdAndStudyMaterialId(UserId userId, StudyMaterialId studyMaterialId);
    Mono<Long> countTotalMatchesByUserId(UserId userId);
    Mono<Long> countByUserIdAndStatus(UserId userId, MatchStatus status);

    //접근권
    Mono<Boolean> hasAccessToMaterial(UserId userId, StudyMaterialId materialId);
    
    //중복 매칭 체크
    Mono<Boolean> existsActiveMatchForSpecificMaterials(UserId requesterId, UserId receiverId, StudyMaterialId requesterMaterialId, StudyMaterialId receiverMaterialId);

}
