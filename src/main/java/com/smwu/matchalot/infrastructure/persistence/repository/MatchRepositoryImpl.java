package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.vo.MatchId;
import com.smwu.matchalot.domain.model.vo.MatchStatus;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.reposiotry.MatchRepository;
import com.smwu.matchalot.infrastructure.persistence.mapper.MatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.smwu.matchalot.domain.model.vo.MatchStatus.*;

@Repository
@RequiredArgsConstructor
public class MatchRepositoryImpl implements MatchRepository {
    private final MatchR2dbcRepository r2dbcRepository;
    private final MatchMapper mapper;

    @Override
    public Mono<Match> save(Match match) {
        var entity = mapper.toEntity(match);
        if (match.getId() == null) {
            entity.setTimestamps();
        } else {
            entity.setUpdatedAt(match.getCreatedAt());
        }
        return r2dbcRepository.save(entity)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Match> findById(MatchId matchId) {
        return r2dbcRepository.findById(matchId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(MatchId matchId) {
        return r2dbcRepository.deleteById(matchId.value());
    }

    @Override
    public Flux<Match> findByUserId(UserId userId) {
        return r2dbcRepository.findByUserId(userId.value())
                .map(mapper::toDomain);
    }



    @Override
    public Flux<Match> findSentRequestsByUser(UserId myId) {
        return r2dbcRepository.findSentRequestsByUser(myId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findByReceiverId(UserId receiverId) {
        return r2dbcRepository.findByUserId(receiverId.value())
                .filter(entity -> entity.getReceiverId().equals(receiverId.value()))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findByRequesterId(UserId requesterId) {
        return r2dbcRepository.findByUserId(requesterId.value())
                .filter(entity -> entity.getRequesterId().equals(requesterId.value()))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findByUserIdInvolved(UserId userId) {
        return r2dbcRepository.findByUserId(userId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Match> findCompletedMatchByUserAndMaterial(UserId userId, StudyMaterialId materialId) {
        return r2dbcRepository.findCompletedMatchByUserAndMaterial(userId.value(), materialId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findPendingMatchesByUserId(UserId userId) {
        return r2dbcRepository.findByUserIdAndStatus(userId.value(), PENDING.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findAcceptedMatchesByUserId(UserId userId) {
        return r2dbcRepository.findByUserIdAndStatus(userId.value(), ACCEPTED.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findCompletedMatchesByUserId(UserId userId) {
        return r2dbcRepository.findByUserIdAndStatus(userId.value(), COMPLETED.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findPendingRequestsToUser(UserId receiverId) {
        return r2dbcRepository.findPendingRequestsToUser(receiverId.value())
                .map(mapper::toDomain);
    }



    @Override
    public Mono<Boolean> existsPendingMatchBetween(UserId userId1, UserId userId2) {
        return r2dbcRepository.existsPendingMatchBetween(userId1.value(), userId2.value());
    }

    @Override
    public Mono<Boolean> existsActiveMAtchForMaterial(StudyMaterialId materialId) {
        return r2dbcRepository.existsActiveMatchForMaterial(materialId.value());
    }


    @Override
    public Flux<Match> findByMaterialIds(StudyMaterialId materialId1, StudyMaterialId materialId2) {
        return r2dbcRepository.findByMaterialIds(materialId1.value(), materialId2.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Match> findExpiredMatches() {
        return r2dbcRepository.findExpiredMatches(LocalDateTime.now())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countByUserIdAndStudyMaterialId(UserId userId, StudyMaterialId studyMaterialId) {
        return r2dbcRepository.countByUserIdAndStudyMaterialId(userId.value(), studyMaterialId.value());
    }
    @Override
    public Mono<Long> countByUserIdAndStatus(UserId userId, MatchStatus status) {
        return r2dbcRepository.countByUserIdAndStatus(userId.value(), status.name());
    }
    @Override
    public Mono<Long> countTotalMatchesByUserId(UserId userId) {
        return r2dbcRepository.countTotalMatchesByUserId(userId.value());
    }


}
