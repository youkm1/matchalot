package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.dto.LoginResponse;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/callback")
    public Mono<LoginResponse> handleCallback(@AuthenticationPrincipal OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // @sookmyung.ac.kr 도메인 체크
        if (!email.endsWith("@sookmyung.ac.kr")) {
            return Mono.error(new IllegalArgumentException(" @sookmyung.ac.kr 이메일만 가능해요!"));
        }

        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .map(user -> new LoginResponse(
                        generateJwtToken(user),
                        toUserResponse(user),
                        false
                ))
                .switchIfEmpty(
                        userService.createUser(userEmail, name)
                                .map(user -> new LoginResponse(
                                        generateJwtToken(user),
                                        toUserResponse(user),
                                        true
                                ))
                );
    }

    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return Mono.error(new IllegalStateException("Not authenticated"));
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .map(this::toUserResponse);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout() {
        // 응답 보내면 클라이언트에서 로컬스토리지 토큰 삭제
        Map<String, String> response = Map.of(
                "message", "로그아웃 성공",
                "action", "클라이언트 스토리지에서도 삭제할것"
        );
        return Mono.just(ResponseEntity.ok(response));
    }

    private String generateJwtToken(User user) {
        return "jwt-token-" + user.getId().value();
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId() != null ? user.getId().value() : null,
                user.getEmail().value(),
                user.getNickname(),
                user.getTrustScore().value(),
                user.getCreatedAt()
        );
    }
}