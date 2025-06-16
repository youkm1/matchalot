package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.MatchId;
import com.smwu.matchalot.domain.model.vo.MatchStatus;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.reposiotry.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final UserService userService;
    private final StudyMaterialService studyMaterialService;

    public Mono<Match> requestMatch(UserId requesterId,StudyMaterialId requesterMaterialId, UserId receiverId) {
        return validateMatchRequest(requesterId, requesterMaterialId,receiverId)
                .flatMap(ignored->findPartnerMaterial(receiverId, requesterMaterialId))
                .flatMap((StudyMaterial partnerMaterial) -> {

                    Match newMatch = new Match(
                            requesterId,
                            receiverId,
                            requesterMaterialId,
                            partnerMaterial.getId()
                    );
                    return matchRepository.save(newMatch);
                });
    }
    public Mono<Match> acceptMatch(MatchId matchId, UserId userId) {
        return matchRepository.findById(matchId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("매칭을 찾을 수 없습니다")))
                .flatMap(match -> {
                    // 권한 체크 - 받는사람만 수락 가능
                    if (!match.isReceiver(userId)) {
                        return Mono.error(new IllegalStateException("매칭을 수락할 권한이 없습니다"));
                    }

                    // 만료 체크
                    if (LocalDateTime.now().isAfter(match.getExpiredAt())) {
                        return matchRepository.save(match.expire())
                                .then(Mono.error(new IllegalStateException("만료된 매칭입니다")));
                    }

                    // 수락 처리
                    return matchRepository.save(match.accept());
                });
    }

    public Flux<StudyMaterial> findPotentialMatches(UserId requesterId, StudyMaterialId studyMaterialId) {
        return studyMaterialService.getStudyMaterial(studyMaterialId)
                .flatMapMany(requesterMaterial ->
                        studyMaterialService.getStudyMaterialsBySubjectAndExamType(
                                requesterMaterial.getSubject(),
                                requesterMaterial.getExamType()
                        )
                )
                .filter(material -> !material.getUploaderId().equals(requesterId))
                .filter(material -> !material.getId().equals(studyMaterialId));

    }

    public Mono<Match> rejectMatch(MatchId matchId, UserId userId) {
        return matchRepository.findById(matchId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("매칭을 찾을 수 없습니다")))
                .flatMap(match -> {
                    if (!match.isReceiver(userId)) {
                        return Mono.error(new IllegalStateException("매칭을 거절할 권한이 없습니다"));
                    }
                    return matchRepository.save(match.reject());
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

                    // 매칭 완료 후 신뢰도 업데이트
                    return matchRepository.save(match.complete())
                            .flatMap(completedMatch -> updateTrustScoresForGoodMatch(match.getRequesterId(), match.getReceiverId())
                                    .then(Mono.just(completedMatch)));
                });
    }
    private Mono<Void> validateRequesterMaterial(UserId requesterId, StudyMaterialId materialId) {
        return studyMaterialService.getStudyMaterial(materialId)
                .flatMap(material -> {
                    if (!material.isUploadedBy(requesterId)) {
                        return Mono.error(new IllegalStateException("본인의 족보만 매칭에 사용할 수 있습니다"));
                    }
                    return Mono.empty();
                });
    }


    private Mono<StudyMaterial> findPartnerMaterial(UserId partnerId, StudyMaterialId requesterMaterialId) {
        return studyMaterialService.getStudyMaterial(requesterMaterialId)
                .flatMap(requesterMaterial ->
                        studyMaterialService.getStudyMaterialsBySubjectAndExamType(
                                        requesterMaterial.getSubject(),
                                        requesterMaterial.getExamType()
                                )
                                .filter(material -> material.isUploadedBy(partnerId))
                                .next()
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("상대방이 해당 과목의 족보를 가지고 있지 않습니다")))
                );
    }
    private Mono<Void> updateTrustScoresForGoodMatch(UserId requesterId, UserId partnerId) {
        return Mono.when(
                userService.updateTrustScore(requesterId, true),
                userService.updateTrustScore(partnerId, true)
        );
    }
    private Mono<Void> validateMatchRequest(UserId requesterId, StudyMaterialId requesterMaterialId, UserId partnerId) {

        if (requesterId.equals(partnerId)) {
            return Mono.error(new IllegalArgumentException("본인과는 매칭할 수 없습니다"));
        }


        return userService.getUserById(requesterId)
                .filter(User::participableInMatch)
                .switchIfEmpty(Mono.error(new IllegalStateException("신뢰도가 부족하여 매칭할 수 없습니다")))
                .then(validateRequesterMaterial(requesterId, requesterMaterialId));
    }

    public Flux<Match> getReceivedRequests(UserId userId) {
        return matchRepository.findPendingRequestsToUser(userId)
                .filter(match -> !LocalDateTime.now().isAfter(match.getExpiredAt()));
    }


    public Flux<Match> getSentRequests(UserId userId) {
        return matchRepository.findSentRequestsByUser(userId)
                .filter(match -> !LocalDateTime.now().isAfter(match.getExpiredAt()));
    }


    public Flux<Match> getMyMatches(UserId userId) {
        return matchRepository.findByUserId(userId);
    }


    public Flux<Match> getActiveMatches(UserId userId) {
        return matchRepository.findByUserId(userId)
                .filter(match -> match.getStatus().isActive())
                .filter(match -> !match.isExpired());
    }


    public Mono<Long> cleanupExpiredMatches() {
        return matchRepository.findExpiredMatches()
                .flatMap(match -> matchRepository.save(match.expire()))
                .count();
    }
}
