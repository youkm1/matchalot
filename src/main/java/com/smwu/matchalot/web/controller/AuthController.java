package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.config.JwtTokenProvider;
import com.smwu.matchalot.web.dto.LoginResponse;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

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

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(
            @AuthenticationPrincipal OAuth2User oauth2User,
            ServerWebExchange exchange) {

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        Email userEmail = Email.of(email);

        return userService.getUserByEmail(userEmail)
                .map(user -> {
                    // 🚀 진짜 JWT 토큰 생성
                    String token = jwtTokenProvider.createToken(
                            user.getId().value().toString(),
                            user.getEmail().value(),
                            user.getNickname()
                    );

                    // 🍪 쿠키 설정
                    setSecureCookie(exchange.getResponse(), "auth-token", token);

                    LoginResponse response = LoginResponse.success(
                            null, // 쿠키로만 전달
                            toUserResponse(user),
                            false // 기존 사용자
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("로그인 중 오류", error);
                    return Mono.just(
                            ResponseEntity.badRequest().body(
                                    LoginResponse.fail("로그인 중 오류가 발생했습니다.")
                            )
                    );
                });
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<LoginResponse>> signup(
            @AuthenticationPrincipal OAuth2User oauth2User,
            ServerWebExchange exchange) { // 🔧 추가!

        if (oauth2User == null) {
            log.error("OAuth2User가 null입니다.");
            return Mono.just(ResponseEntity.badRequest()
                    .body(LoginResponse.fail("인증되지 않은 사용자입니다.")));
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
                                .map(user -> {
                                    // 🚀 진짜 JWT 토큰 생성
                                    String token = jwtTokenProvider.createToken(
                                            user.getId().value().toString(),
                                            user.getEmail().value(),
                                            user.getNickname()
                                    );

                                    setSecureCookie(exchange.getResponse(), "auth-token", token);

                                    return LoginResponse.success(
                                            null,
                                            toUserResponse(user),
                                            false // 기존 사용자
                                    );
                                });
                    }

                    // 신규 사용자 생성
                    return userService.createUser(userEmail, name)
                            .map(newUser -> {
                                log.info("회원가입 성공: {}", newUser.getEmail().value());

                                // 🚀 진짜 JWT 토큰 생성
                                String token = jwtTokenProvider.createToken(
                                        newUser.getId().value().toString(),
                                        newUser.getEmail().value(),
                                        newUser.getNickname()
                                );

                                setSecureCookie(exchange.getResponse(), "auth-token", token);

                                return LoginResponse.success(
                                        null,
                                        toUserResponse(newUser),
                                        true // 신규 사용자
                                );
                            })
                            .onErrorResume(org.springframework.dao.DuplicateKeyException.class, ex -> {
                                log.warn("중복 키 에러, 기존 사용자로 처리: {}", email);
                                return userService.getUserByEmail(userEmail)
                                        .map(user -> {
                                            String token = jwtTokenProvider.createToken(
                                                    user.getId().value().toString(),
                                                    user.getEmail().value(),
                                                    user.getNickname()
                                            );

                                            setSecureCookie(exchange.getResponse(), "auth-token", token);

                                            return LoginResponse.success(
                                                    null,
                                                    toUserResponse(user),
                                                    false
                                            );
                                        });
                            });
                })
                .map(ResponseEntity::ok) // 🔧 ResponseEntity로 감싸기
                .onErrorResume(error -> {
                    log.error("회원가입 처리 중 오류", error);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(LoginResponse.fail("회원가입 처리 중 오류가 발생했습니다.")));
                });
    }

    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) { // 🔧 반환 타입 수정
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

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout(ServerWebExchange exchange) { // 🔧 파라미터 추가
        log.info("로그아웃 요청 처리");

        // 🍪 쿠키 삭제
        deleteSecureCookie(exchange.getResponse(), "auth-token");

        Map<String, Object> response = Map.of(
                "message", "로그아웃 성공"
        );
        return Mono.just(ResponseEntity.ok(response));
    }


    private void setSecureCookie(ServerHttpResponse response, String name, String value) {
        String cookieValue = String.format(
                "%s=%s; HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/",
                name, value
        );
        response.getHeaders().add("Set-Cookie", cookieValue);
    }


    private void deleteSecureCookie(ServerHttpResponse response, String name) {
        String cookieValue = String.format(
                "%s=; HttpOnly; Secure; SameSite=Strict; Max-Age=0; Path=/",
                name
        );
        response.getHeaders().add("Set-Cookie", cookieValue);
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