package com.smwu.matchalot.application.service;

import com.smwu.matchalot.application.event.MatchEvent;
import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.domain.repository.MatchRepository;
import com.smwu.matchalot.domain.repository.StudyMaterialRepository;
import com.smwu.matchalot.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionalOperator transactionalOperator;
    private final NotificationService notificationService;

    public Mono<Match> requestMatch(UserId requesterId, StudyMaterialId requesterMaterialId, UserId receiverId, StudyMaterialId receiverMaterialId) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 requestMatch 시작 - requesterId: {}, requesterMaterialId: {}, receiverId: {}, receiverMaterialId: {}", 
            requesterId.value(), requesterMaterialId.value(), receiverId.value(), receiverMaterialId.value());
        
        return transactionalOperator.transactional(validateMatchRequest(requesterId, requesterMaterialId, receiverId)
                .doOnSuccess(v -> log.info("⏱️ Validation completed in {}ms", 
                    System.currentTimeMillis() - startTime))
                .doOnError(ex -> log.error("❌ Validation 실패: {}", ex.getMessage()))
                .then(Mono.just(receiverMaterialId))
                .doOnNext(m -> log.info("⏱️ Receiver material ID validated in {}ms", 
                    System.currentTimeMillis() - startTime))
                .flatMap(receiverMatId -> {
                    Match newMatch = new Match(requesterId, receiverId, requesterMaterialId, receiverMatId);
                    return matchRepository.save(newMatch)
                            .doOnNext(match -> log.info("💾 Match 저장 완료 - id: {}, status: {}", 
                                match.getId() != null ? match.getId().value() : "null", match.getStatus()))
                            .flatMap(match -> {
                                long eventStart = System.currentTimeMillis();
                                log.info("⏱️ Match saved to DB in {}ms", eventStart - startTime);
                                
                                if (match.getId() == null) {
                                    log.error("❌ Match ID가 null입니다!");
                                    return Mono.error(new IllegalStateException("매칭 ID 생성 실패"));
                                }
                                
                                // 매칭 요청받은 사용자에게 알림 전송
                                return userRepository.findById(requesterId)
                                        .flatMap(requester -> 
                                                notificationService.notifyMatchRequestReceived(
                                                        receiverId,
                                                        requester.getNickname(),
                                                        match.getId().value()
                                                ))
                                        .then(Mono.just(match))
                                        .doOnSuccess(m -> {
                                            log.info("⏱️ Notification sent in {}ms", 
                                                System.currentTimeMillis() - eventStart);
                                            log.info("✅ Total match request processing time: {}ms", 
                                                System.currentTimeMillis() - startTime);
                                        });
                            });
                })
        )       .doOnError(error -> log.error("매칭 요청 실패", error));
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
        return transactionalOperator.transactional(
            matchRepository.findById(matchId)
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

                                // 양쪽 사용자에게 매칭 완료 알림 생성
                                Mono<Void> notifyRequester = userRepository.findById(match.getReceiverId())
                                        .flatMap(receiver -> studyMaterialRepository.findById(match.getRequesterMaterialId())
                                                .flatMap(material -> notificationService.notifyMatchCompleted(
                                                        match.getRequesterId(),
                                                        receiver.getNickname(),
                                                        material.getTitle(),
                                                        m.getId().value()
                                                )))
                                        .then();
                                
                                Mono<Void> notifyReceiver = userRepository.findById(match.getRequesterId())
                                        .flatMap(requester -> studyMaterialRepository.findById(match.getReceiverMaterialId())
                                                .flatMap(material -> notificationService.notifyMatchCompleted(
                                                        match.getReceiverId(),
                                                        requester.getNickname(),
                                                        material.getTitle(),
                                                        m.getId().value()
                                                )))
                                        .then();

                                return Mono.when(updateRequesterScore, updateReceiverScore, notifyRequester, notifyReceiver)
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
                })
        );
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
        log.info("🔍 validateMatchRequest 시작 - requesterId: {}, materialId: {}, partnerId: {}", 
            requesterId.value(), requesterMaterialId.value(), partnerId.value());
            
        if (requesterId.equals(partnerId)) {
            log.error("❌ 본인과 매칭 시도: requesterId={}, partnerId={}", requesterId.value(), partnerId.value());
            return Mono.error(new IllegalArgumentException("본인과는 매칭할 수 없습니다"));
        }

        return userRepository.findById(requesterId)
                .doOnNext(user -> log.info("✅ 요청자 조회 성공: id={}, participable={}", 
                    user.getId().value(), user.participableInMatch()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("요청자 정보를 찾을 수 없습니다")))
                .flatMap(requester -> {
                    if (!requester.participableInMatch()) {
                        log.error("❌ 매칭 참여 불가: trustScore={}", requester.getTrustScore().value());
                        return Mono.error(new IllegalStateException("매칭에 참여할 수 없는 상태입니다. 신뢰도를 확인해주세요."));
                    }
                    log.info("✅ validateMatchRequest 완료");
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
        return transactionalOperator.transactional(
            matchRepository.findExpiredMatches()
                .flatMap(match -> matchRepository.save(match.expire()))
                .count()
        );
    }
}