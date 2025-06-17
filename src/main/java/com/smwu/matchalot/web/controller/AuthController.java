package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.dto.LoginResponse;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/callback")
    public Mono<ResponseEntity<LoginResponse>> handleCallback(@AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            log.warn("OAuth2User is null");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, false, "인증 정보가 없습니다.")));
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        log.info("OAuth2 callback received for email: {}", email);

        // 숙명대 도메인 체크
        if (email == null || !email.endsWith("@sookmyung.ac.kr")) {
            log.warn("Invalid domain attempt: {}", email);
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new LoginResponse(null, null, false, "숙명여자대학교 구글 계정(@sookmyung.ac.kr)만 사용 가능합니다.")));
        }

        try {
            Email userEmail = Email.of(email);

            return userService.getUserByEmail(userEmail)
                    .map(user -> {
                        // 기존 사용자
                        String token = generateJwtToken(user);
                        return ResponseEntity.ok(new LoginResponse(
                                token,
                                toUserResponse(user),
                                false,
                                "로그인 성공"
                        ));
                    })
                    .switchIfEmpty(
                            // 신규 사용자 생성
                            userService.createUser(userEmail, name)
                                    .map(user -> {
                                        String token = generateJwtToken(user);
                                        return ResponseEntity.status(HttpStatus.CREATED)
                                                .body(new LoginResponse(
                                                        token,
                                                        toUserResponse(user),
                                                        true,
                                                        "회원가입 및 로그인 성공"
                                                ));
                                    })
                    )
                    .onErrorResume(throwable -> {
                        log.error("Error during auth callback: ", throwable);
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new LoginResponse(null, null, false, "서버 오류가 발생했습니다.")));
                    });

        } catch (Exception e) {
            log.error("Email validation failed for: {}", email, e);
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new LoginResponse(null, null, false, "유효하지 않은 이메일 형식입니다.")));
        }
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .map(user -> ResponseEntity.ok(toUserResponse(user)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout() {
        Map<String, String> response = Map.of(
                "message", "로그아웃 성공",
                "action", "클라이언트에서 토큰을 삭제해주세요"
        );
        return Mono.just(ResponseEntity.ok(response));
    }

    private String generateJwtToken(User user) {
        // 실제로는 JWT 라이브러리 사용
        return "jwt-token-" + user.getId().value() + "-" + System.currentTimeMillis();
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId() != null ? user.getId().value() : null,
                user.getNickname(),
                user.getEmail().value(),
                user.getTrustScore().value(),
                user.getCreatedAt()
        );
    }
}