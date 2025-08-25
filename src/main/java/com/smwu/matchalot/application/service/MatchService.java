package com.smwu.matchalot.application.service;

import com.smwu.matchalot.application.event.MatchEvent;
import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.reposiotry.MatchRepository;
import com.smwu.matchalot.domain.reposiotry.StudyMaterialRepository;
import com.smwu.matchalot.domain.reposiotry.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public Mono<Match> requestMatch(UserId requesterId, StudyMaterialId requesterMaterialId, UserId receiverId) {
        long startTime = System.currentTimeMillis();
        
        return validateMatchRequest(requesterId, requesterMaterialId, receiverId)
                .doOnNext(v -> log.info("⏱️ Validation completed in {}ms", 
                    System.currentTimeMillis() - startTime))
                .flatMap(ignored -> findPartnerMaterial(receiverId, requesterMaterialId))
                .doOnNext(m -> log.info("⏱️ Partner material found in {}ms", 
                    System.currentTimeMillis() - startTime))
                .flatMap(partnerMaterial -> {
                    Match newMatch = new Match(requesterId, receiverId, requesterMaterialId, partnerMaterial.getId());
                    return matchRepository.save(newMatch)
                            .doOnNext(match -> {
                                long eventStart = System.currentTimeMillis();
                                log.info("⏱️ Match saved to DB in {}ms", eventStart - startTime);
                                
                                eventPublisher.publishEvent(new MatchEvent(
                                        this,
                                        receiverId.value().toString(),
                                        "MATCH_REQUEST",
                                        Map.of(
                                                "matchId", match.getId().value(),
                                                "requesterId", requesterId.value(),
                                                "requesterMaterialId", requesterMaterialId.value()
                                        )
                                ));
                                
                                log.info("⏱️ Event published in {}ms", 
                                    System.currentTimeMillis() - eventStart);
                                log.info("✅ Total match request processing time: {}ms", 
                                    System.currentTimeMillis() - startTime);
                            });
                })
                .doOnError(error -> log.error("매칭 요청 실패", error));
    }

    public Mono<Match> acceptMatch(MatchId matchId, UserId userId) {
        long startTime = System.currentTimeMillis();
        
        return matchRepository.findById(matchId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("매칭을 찾을 수 없습니다")))
                .flatMap(match -> {
                    if (!match.getReceiverId().equals(userId)) {
                        return Mono.error(new IllegalStateException("매칭을 수락할 권한이 없습니다"));
                    }
                    Match acceptedMatch = match.accept();
                    return matchRepository.save(acceptedMatch)
                            .doOnNext(m -> {
                                long eventStart = System.currentTimeMillis();
                                log.info("Match accepted and saved in {}ms",
                                    eventStart - startTime);
                                
                                eventPublisher.publishEvent(new MatchEvent(
                                        this,
                                        match.getRequesterId().value().toString(),
                                        "MATCH_ACCEPTED",
                                        Map.of(
                                                "matchId", m.getId().value(),
                                                "accepterId", userId.value()
                                        )
                                ));
                                
                                log.info("Accept event published in {}ms",
                                    System.currentTimeMillis() - eventStart);
                                log.info("Total match accept processing time: {}ms",
                                    System.currentTimeMillis() - startTime);
                            });
                });
    }

    public Mono<Match> rejectMatch(MatchId matchId, UserId userId) {
        return matchRepository.findById(matchId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("매칭을 찾을 수 없습니다")))
                .flatMap(match -> {
                    if (!match.getReceiverId().equals(userId)) {
                        return Mono.error(new IllegalStateException("매칭을 거절할 권한이 없습니다"));
                    }
                    Match rejectedMatch = match.reject();
                    return matchRepository.save(rejectedMatch)
                            .doOnNext(m -> {
                                eventPublisher.publishEvent(new MatchEvent(
                                        this,
                                        match.getRequesterId().value().toString(),
                                        "MATCH_REJECTED",
                                        Map.of(
                                                "matchId", m.getId().value(),
                                                "rejecterId", userId.value()
                                        )
                                ));
                            });
                });
    }

    public Mono<Match> completeMatch(MatchId matchId, UserId userId) {
        return matchRepository.findById(matchId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("매칭을 찾을 수 없습니다")))
                .flatMap(match -> {
                    if (!match.isParticipant(userId)) {
                        return Mono.error(new IllegalStateException("매칭 참여자가 아닙니다"));
                    }
                    if (match.getStatus() != MatchStatus.ACCEPTED) {
                        return Mono.error(new IllegalStateException("수락된 매칭만 완료할 수 있습니다"));
                    }

                    Match completedMatch = match.complete();
                    return matchRepository.save(completedMatch)
                            .flatMap(m -> {
                                Mono<Void> updateRequesterScore = userService.updateTrustScore(
                                        match.getRequesterId(), true).then();
                                Mono<Void> updateReceiverScore = userService.updateTrustScore(
                                        match.getReceiverId(), true).then();

                                return Mono.when(updateRequesterScore, updateReceiverScore)
                                        .thenReturn(m);
                            })
                            .doOnNext(m -> {
                                // 매칭 완료 이벤트 발행 (양쪽 모두에게 알림)
                                String otherUserId = userId.equals(match.getRequesterId()) 
                                    ? match.getReceiverId().value().toString()
                                    : match.getRequesterId().value().toString();
                                
                                eventPublisher.publishEvent(new MatchEvent(
                                        this,
                                        otherUserId,
                                        "MATCH_COMPLETED",
                                        Map.of(
                                                "matchId", m.getId().value(),
                                                "completedBy", userId.value(),
                                                "trustScoreUpdated", true
                                        )
                                ));
                                
                                log.info("✅ 매칭 완료: matchId={}, completedBy={}", 
                                    m.getId().value(), userId.value());
                            });
                });
    }

    private Mono<StudyMaterial> findPartnerMaterial(UserId partnerId, StudyMaterialId requesterMaterialId) {
        return studyMaterialRepository.findById(requesterMaterialId)
                .flatMap(requesterMaterial -> {
                    return studyMaterialRepository.findByUploaderIdAndSubjectAndExamType(
                            partnerId,
                            requesterMaterial.getSubject(),
                            requesterMaterial.getExamType()
                    ).next()
                            .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                    "매칭 상대방이 해당 과목과 시험 유형의 자료를 보유하지 않습니다"
                            )));
                });
    }

    public Flux<StudyMaterial> findPotentialMatches(UserId userId, StudyMaterialId materialId) {
        return studyMaterialRepository.findById(materialId)
                .flatMapMany(material -> {
                    return studyMaterialRepository.findBySubjectAndExamType(
                            material.getSubject(),
                            material.getExamType()
                    ).filter(m -> !m.getUploaderId().equals(userId));
                });
    }

    public Flux<Match> getReceivedRequests(UserId userId) {
        return matchRepository.findByReceiverId(userId)
                .filter(match -> match.getStatus() == MatchStatus.PENDING);
    }

    public Flux<Match> getSentRequests(UserId userId) {
        return matchRepository.findByRequesterId(userId)
                .filter(match -> match.getStatus() == MatchStatus.PENDING);
    }

    public Flux<Match> getMyMatches(UserId userId) {
        return matchRepository.findByUserIdInvolved(userId);
    }

    private Mono<Void> validateMatchRequest(UserId requesterId, StudyMaterialId requesterMaterialId, UserId partnerId) {
        if (requesterId.equals(partnerId)) {
            return Mono.error(new IllegalArgumentException("본인과는 매칭할 수 없습니다"));
        }

        return userRepository.findById(requesterId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("요청자 정보를 찾을 수 없습니다")))
                .flatMap(requester -> {
                    if (!requester.participableInMatch()) {
                        return Mono.error(new IllegalStateException("매칭에 참여할 수 없는 상태입니다. 신뢰도를 확인해주세요."));
                    }
                    return Mono.empty();
                });
    }

    public Mono<Boolean> hasCompletedMatch(UserId userId, StudyMaterialId materialId) {
        return matchRepository.findCompletedMatchByUserAndMaterial(userId, materialId)
                .hasElement();
    }

    public Flux<Match> getActiveMatches(UserId userId) {
        return matchRepository.findByUserIdInvolved(userId)
                .filter(match -> match.getStatus().isActive())
                .filter(match -> !match.isExpired());
    }


    public Mono<Long> cleanupExpiredMatches() {
        return matchRepository.findExpiredMatches()
                .flatMap(match -> matchRepository.save(match.expire()))
                .count();
    }
}