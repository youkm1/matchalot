package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.dto.LoginResponse;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * OAuth2 인증 완료 후 사용자 상태 확인
     * 기존 사용자인지 신규 사용자인지 판단하여 적절한 엔드포인트로 안내
     */
    @GetMapping("/callback")
    public Mono<Map<String, Object>> checkUserStatus(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            log.error("OAuth2User가 null입니다.");
            return Mono.just(Map.<String, Object>of(
                    "status", "error",
                    "message", "인증되지 않은 사용자입니다."
            ));
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        log.info("OAuth2 콜백 - 사용자 상태 확인: 이메일={}, 이름={}", email, name);

        // 이메일 도메인 체크
        if (email == null || !email.endsWith("@sookmyung.ac.kr")) {
            log.warn("허용되지 않은 도메인: {}", email);
            return Mono.just(Map.<String, Object>of(
                    "status", "error",
                    "message", "@sookmyung.ac.kr 이메일만 가능해요!"
            ));
        }

        Email userEmail = Email.of(email);

        // 기존 사용자 여부 확인
        return userService.existsByEmail(userEmail)
                .map(userExists -> {
                    if (userExists) {
                        log.info("기존 사용자 확인: {}", email);
                        return Map.<String, Object>of(
                                "status", "existing_user",
                                "message", "기존 사용자입니다. 로그인을 진행하세요.",
                                "email", email,
                                "nickname", name,
                                "action", "login"
                        );
                    } else {
                        log.info("신규 사용자 확인: {}", email);
                        return Map.<String, Object>of(
                                "status", "new_user",
                                "message", "신규 사용자입니다. 회원가입을 진행하세요.",
                                "email", email,
                                "nickname", name,
                                "action", "signup"
                        );
                    }
                })
                .onErrorResume(error -> {
                    log.error("사용자 상태 확인 중 오류", error);
                    return Mono.just(Map.<String, Object>of(
                            "status", "error",
                            "message", "사용자 상태 확인 중 오류가 발생했습니다."
                    ));
                });
    }

    /**
     * 기존 사용자 로그인
     */
    @PostMapping("/login")
    public Mono<LoginResponse> login(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            log.error("OAuth2User가 null입니다.");
            return Mono.just(LoginResponse.fail("인증되지 않은 사용자입니다."));
        }

        String email = oauth2User.getAttribute("email");
        log.info("로그인 시도: {}", email);

        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .map(user -> {
                    log.info("로그인 성공: {}", user.getEmail().value());
                    return LoginResponse.success(
                            generateJwtToken(user),
                            toUserResponse(user),
                            false // 기존 사용자
                    );
                })
                .switchIfEmpty(Mono.just(LoginResponse.fail("사용자를 찾을 수 없습니다. 먼저 회원가입을 진행해주세요.")))
                .onErrorResume(error -> {
                    log.error("로그인 처리 중 오류", error);
                    return Mono.just(LoginResponse.fail("로그인 처리 중 오류가 발생했습니다."));
                });
    }

    /**
     * 신규 사용자 회원가입
     */
    @PostMapping("/signup")
    public Mono<LoginResponse> signup(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            log.error("OAuth2User가 null입니다.");
            return Mono.just(LoginResponse.fail("인증되지 않은 사용자입니다."));
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        log.info("회원가입 시도: 이메일={}, 이름={}", email, name);

        Email userEmail = Email.of(email);

        return userService.existsByEmail(userEmail)
                .flatMap(userExists -> {
                    if (userExists) {
                        log.info("이미 가입된 사용자 발견: {}", email);
                        // 기존 사용자로 처리하고 로그인 진행
                        return userService.getUserByEmail(userEmail)
                                .map(user -> LoginResponse.success(
                                        generateJwtToken(user),
                                        toUserResponse(user),
                                        false // 기존 사용자
                                ));
                    }

                    // 신규 사용자 생성
                    return userService.createUser(userEmail, name)
                            .map(newUser -> {
                                log.info("회원가입 성공: {}", newUser.getEmail().value());
                                return LoginResponse.success(
                                        generateJwtToken(newUser),
                                        toUserResponse(newUser),
                                        true // 신규 사용자
                                );
                            })
                            .onErrorResume(org.springframework.dao.DuplicateKeyException.class, ex -> {
                                log.warn("중복 키 에러, 기존 사용자로 처리: {}", email);
                                return userService.getUserByEmail(userEmail)
                                        .map(user -> LoginResponse.success(
                                                generateJwtToken(user),
                                                toUserResponse(user),
                                                false
                                        ));
                            });
                })
                .onErrorResume(error -> {
                    log.error("회원가입 처리 중 오류", error);
                    return Mono.just(LoginResponse.fail("회원가입 처리 중 오류가 발생했습니다."));
                });
    }

    /**
     * 현재 로그인된 사용자 정보 조회
     */
    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return Mono.error(new IllegalStateException("인증되지 않은 사용자입니다."));
        }

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        log.info("현재 사용자 정보 조회: {}", email);

        return userService.getUserByEmail(userEmail)
                .map(this::toUserResponse)
                .switchIfEmpty(Mono.error(new IllegalStateException("사용자를 찾을 수 없습니다.")));
    }

    /**
     * 로그아웃 처리
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout() {
        log.info("로그아웃 요청 처리");

        Map<String, Object> response = Map.of(
                "message", "로그아웃 성공",
                "action", "클라이언트에서 토큰을 삭제하세요"
        );
        return Mono.just(ResponseEntity.ok(response));
    }

    // === Private Helper Methods ===

    private String generateJwtToken(User user) {
        return "jwt-token-" + user.getId().value();
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