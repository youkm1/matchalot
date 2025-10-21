package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.MatchService;
import com.smwu.matchalot.application.service.StudyMaterialService;
import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.MatchId;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.web.dto.MatchRequestDto;
import com.smwu.matchalot.web.dto.MatchResponse;
import com.smwu.matchalot.web.dto.StudyMaterialSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final MatchService matchService;
    private final UserService userService;
    private final StudyMaterialService studyMaterialService;

    @PostMapping("/request/{materialId}")
    public Mono<ResponseEntity<MatchResponse>> requestMatch(
            @PathVariable("materialId") Long materialId,
            @Valid @RequestBody MatchRequestDto request,
            @AuthenticationPrincipal OAuth2User oAuth2User) {


        log.info("매칭 요청 수신: materialId={}", materialId);

        if (oAuth2User == null) {
            log.error("OAuth2User가 null입니다");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            log.error("사용자 이메일이 null입니다");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        log.info("요청자 이메일: {}", email);
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> {
                    log.info("사용자 조회 성공: {}", user.getId().value());
                    
                    // receiverMaterialId가 null이면 URL의 materialId 사용
                    StudyMaterialId receiverMaterialId = request.receiverMaterialId() != null 
                        ? StudyMaterialId.of(request.receiverMaterialId())
                        : StudyMaterialId.of(materialId);
                        
                    log.info("매칭 서비스 호출 전: requesterId={}, requesterMaterialId={}, receiverId={}, receiverMaterialId={}", 
                        user.getId().value(), request.requesterMaterialId(), request.getReceiverId().value(), receiverMaterialId.value());
                        
                    return matchService.requestMatch(
                            user.getId(),
                            request.getRequesterMaterialId(),
                            request.getReceiverId(),
                            receiverMaterialId
                    );
                })
                .doOnNext(match -> log.info("매칭 생성 성공: {}", match.getId()))
                .flatMap(match -> userService.getUserByEmail(userEmail)
                        .flatMap(user -> toMatchResponse(match, user.getId())))
                .doOnNext(response -> log.info("응답 생성 성공: {}", response.matchId()))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, ex -> {
                    log.error("잘못된 요청: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(IllegalStateException.class, ex -> {
                    log.error("권한 오류: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                })
                .doOnError(ex -> log.error("🚨 매칭 요청 실패 - 에러 타입: {}, 메시지: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex))
                .onErrorResume(Exception.class, ex -> {  // 🎯 모든 예외 처리
                    log.error("매칭 요청 처리 중 예상치 못한 오류: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/potential/{materialId}")
    public Flux<StudyMaterialSummaryResponse> getPotentialPartners(
            @AuthenticationPrincipal OAuth2User oAuth2User, @PathVariable("materialId") Long materialId) {

        String email = oAuth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId studyMaterialId = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.findPotentialMatches(user.getId(), studyMaterialId))
                .flatMap(this::toStudyMaterialSummaryResponse);
    }

    @PutMapping("/{matchId}/accept")
    public Mono<ResponseEntity<MatchResponse>> acceptMatch(
            @PathVariable("matchId") Long matchId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        MatchId id = MatchId.of(matchId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> matchService.acceptMatch(id, user.getId())
                        .flatMap(match -> toMatchResponse(match, user.getId())))
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.notFound().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    /**
     * 매칭 거절
     */
    @PutMapping("/{matchId}/reject")
    public Mono<ResponseEntity<MatchResponse>> rejectMatch(
            @PathVariable("matchId") Long matchId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        MatchId id = MatchId.of(matchId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> matchService.rejectMatch(id, user.getId())
                        .flatMap(match -> toMatchResponse(match, user.getId())))
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.notFound().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
    @PutMapping("/{matchId}/complete")
    public Mono<ResponseEntity<MatchResponse>> completeMatch(
            @PathVariable("matchId") Long matchId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        MatchId id = MatchId.of(matchId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> matchService.completeMatch(id, user.getId())
                        .flatMap(match -> toMatchResponse(match, user.getId())))
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.notFound().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
    @GetMapping("/received")
    public Flux<MatchResponse> getReceivedRequests(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.getReceivedRequests(user.getId())
                        .flatMap(match -> toMatchResponse(match, user.getId())));
    }
    @GetMapping("/sent")
    public Flux<MatchResponse> getSentRequests(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.getSentRequests(user.getId())
                        .flatMap(match -> toMatchResponse(match, user.getId())));
    }

    /**
     * 내 모든 매칭 히스토리
     */
    @GetMapping("/my")
    public Flux<MatchResponse> getMyMatches(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.getMyMatches(user.getId())
                        .flatMap(match -> toMatchResponse(match, user.getId())));
    }

    /**
     * 진행 중인 매칭들
     */
    @GetMapping("/active")
    public Flux<MatchResponse> getActiveMatches(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.getActiveMatches(user.getId())
                        .flatMap(match -> toMatchResponse(match, user.getId())));
    }

    @PostMapping("/cleanup")
    public Mono<ResponseEntity<Map<String, Object>>> cleanupExpiredMatches() {
        return matchService.cleanupExpiredMatches()
                .map(count -> ResponseEntity.ok(Map.of(
                        "message", "만료된 매칭을 정리했습니다",
                        "cleanedCount", count
                )));
    }
    private Mono<StudyMaterialSummaryResponse> toStudyMaterialSummaryResponse(StudyMaterial studyMaterial) {
        return userService.getUserById(studyMaterial.getUploaderId())  // Mono<User> 반환
                .map(uploader -> StudyMaterialSummaryResponse.from(  // uploader는 User 객체
                        studyMaterial,
                        uploader.getNickname(),           // User.getNickname() ✅
                        uploader.getTrustScore().value()  // User.getTrustScore() ✅
                ))
                .switchIfEmpty(Mono.just(StudyMaterialSummaryResponse.from(studyMaterial, "알 수 없음", 0)));
    }
    private Mono<String> getUserNickname(UserId userId) {
        return userService.getUserById(userId)
                .map(user -> user.getNickname())
                .switchIfEmpty(Mono.just("알 수 없는 이용자"));
    }

    private Mono<MatchResponse> toMatchResponse(Match match, UserId currentUserId) {
        log.info("🔍 toMatchResponse 시작 - matchId: {}, requesterId: {}, receiverId: {}, requesterMaterialId: {}, receiverMaterialId: {}", 
            match.getId() != null ? match.getId().value() : "null", 
            match.getRequesterId().value(), 
            match.getReceiverId().value(),
            match.getRequesterMaterialId().value(),
            match.getReceiverMaterialId().value());
        
        // 현재 사용자가 requester인지 receiver인지 확인하여 partnerMaterialId 결정
        Long partnerMaterialId = match.getRequesterId().equals(currentUserId) 
            ? match.getReceiverMaterialId().value() 
            : match.getRequesterMaterialId().value();
            
        return Mono.zip(
                getUserNickname(match.getRequesterId()),
                getUserNickname(match.getReceiverId()),
                getStudyMaterialTitle(match.getRequesterMaterialId()),
                getStudyMaterialTitle(match.getReceiverMaterialId())
        ).map(tuple -> {
            log.info("✅ 매칭 응답 데이터 - requesterNick: {}, receiverNick: {}, requesterTitle: {}, receiverTitle: {}, partnerMaterialId: {}", 
                tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4(), partnerMaterialId);
            return MatchResponse.from(
                match,
                tuple.getT1(), // requester nickname
                tuple.getT2(), // partner nickname
                tuple.getT3(), // requester material title
                tuple.getT4(), // partner material title
                partnerMaterialId // 현재 사용자 기준 상대방 자료 ID
            );
        });
    }
    private Mono<String> getStudyMaterialTitle(StudyMaterialId materialId) {
        log.info("📚 자료 제목 조회 시작 - materialId: {}", materialId.value());
        return studyMaterialService.getStudyMaterial(materialId)
                .doOnNext(material -> log.info("✅ 자료 조회 성공 - id: {}, title: {}", 
                    materialId.value(), material.getTitle()))
                .map(material -> material.getTitle())
                .onErrorResume(ex -> {
                    log.error("❌ 자료 조회 실패 - materialId: {}, error: {}", materialId.value(), ex.getMessage());
                    return Mono.just("조회 실패");
                })
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.warn("⚠️ 자료를 찾을 수 없음 - materialId: {}", materialId.value());
                    return "알 수 없음";
                }));
    }

}
