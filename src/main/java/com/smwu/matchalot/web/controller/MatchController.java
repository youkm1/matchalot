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
public class MatchController {

    private final MatchService matchService;
    private final UserService userService;
    private final StudyMaterialService studyMaterialService;

    @PostMapping("/request")
    public Mono<ResponseEntity<MatchResponse>> requestMatch(
            @Valid @RequestBody MatchRequestDto request,
            @AuthenticationPrincipal OAuth2User oAuth2User) {

        String email = oAuth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> matchService.requestMatch(
                        user.getId(),
                        request.getRequesterMaterialId(),
                        request.getReceiverId()
                ))
                .flatMap(this::toMatchResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @GetMapping("/potential/{materialId}")
    public Flux<StudyMaterialSummaryResponse> getPotentialPartners(
            @AuthenticationPrincipal OAuth2User oAuth2User, @PathVariable Long materialId) {

        String email = oAuth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        StudyMaterialId studyMaterialId = StudyMaterialId.of(materialId);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.findPotentialMatches(user.getId(), studyMaterialId))
                .flatMap(this::toStudyMaterialSummaryResponse);
    }

    @PutMapping("/{matchId}/accept")
    public Mono<ResponseEntity<MatchResponse>> acceptMatch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        MatchId id = MatchId.of(matchId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> matchService.acceptMatch(id, user.getId()))
                .flatMap(this::toMatchResponse)
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
            @PathVariable Long matchId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        MatchId id = MatchId.of(matchId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> matchService.rejectMatch(id, user.getId()))
                .flatMap(this::toMatchResponse)
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.notFound().build())
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
    @PutMapping("/{matchId}/complete")
    public Mono<ResponseEntity<MatchResponse>> completeMatch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);
        MatchId id = MatchId.of(matchId);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> matchService.completeMatch(id, user.getId()))
                .flatMap(this::toMatchResponse)
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
                .flatMapMany(user -> matchService.getReceivedRequests(user.getId()))
                .flatMap(this::toMatchResponse);
    }
    @GetMapping("/sent")
    public Flux<MatchResponse> getSentRequests(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.getSentRequests(user.getId()))
                .flatMap(this::toMatchResponse);
    }

    /**
     * 내 모든 매칭 히스토리
     */
    @GetMapping("/my")
    public Flux<MatchResponse> getMyMatches(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.getMyMatches(user.getId()))
                .flatMap(this::toMatchResponse);
    }

    /**
     * 진행 중인 매칭들
     */
    @GetMapping("/active")
    public Flux<MatchResponse> getActiveMatches(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .flatMapMany(user -> matchService.getActiveMatches(user.getId()))
                .flatMap(this::toMatchResponse);
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
        return getUserNickname(studyMaterial.getUploaderId())
                .map(nickname -> StudyMaterialSummaryResponse.from(studyMaterial, nickname));
    }
    private Mono<String> getUserNickname(UserId userId) {
        return userService.getUserById(userId)
                .map(user -> user.getNickname())
                .switchIfEmpty(Mono.just("알 수 없는 이용자"));
    }

    private Mono<MatchResponse> toMatchResponse(Match match) {
        return Mono.zip(
                getUserNickname(match.getRequesterId()),
                getUserNickname(match.getReceiverId()),
                getStudyMaterialTitle(match.getRequesterMaterialId()),
                getStudyMaterialTitle(match.getReceiverMaterialId())
        ).map(tuple -> MatchResponse.from(
                match,
                tuple.getT1(), // requester nickname
                tuple.getT2(), // partner nickname
                tuple.getT3(), // requester material title
                tuple.getT4()  // partner material title
        ));
    }
    private Mono<String> getStudyMaterialTitle(StudyMaterialId materialId) {
        return studyMaterialService.getStudyMaterial(materialId)
                .map(material -> material.getTitle())
                .switchIfEmpty(Mono.just("알 수 없음"));
    }

}
