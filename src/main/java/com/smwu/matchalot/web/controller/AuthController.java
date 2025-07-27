package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.config.JwtTokenProvider;
import com.smwu.matchalot.web.config.OAuth2JwtAuthenticationSuccessHandler;
import com.smwu.matchalot.web.dto.LoginResponse;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.csrf.CsrfToken;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;



    @GetMapping("/callback")
    public Mono<Map<String, Object>> checkUserStatus(ServerWebExchange exchange) {
        return exchange.getSession()
                .mapNotNull(webSession -> webSession.getAttribute("oauth2User"))
                .cast(OAuth2User.class)
                .flatMap(oauth2User -> {
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
                                            "message", "기존 사용자입니다. 로그인이 완료되었습니다.",
                                            "email", email,
                                            "nickname", name,
                                            "action", "login_complete"
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
                            });
                })
                .switchIfEmpty(Mono.just(Map.<String, Object>of(
                        "status", "error",
                        "message", "인증 정보가 없습니다. 다시 로그인해주세요."
                )))
                .onErrorResume(error -> {
                    log.error("사용자 상태 확인 중 오류", error);
                    return Mono.just(Map.<String, Object>of(
                            "status", "error",
                            "message", "사용자 상태 확인 중 오류가 발생했습니다."
                    ));
                });
    }


    @PostMapping("/signup")
    public Mono<ResponseEntity<LoginResponse>> signup(ServerWebExchange exchange) {
        return exchange.getSession()
                .mapNotNull(webSession -> webSession.getAttribute("oauth2User"))
                .cast(OAuth2User.class)
                .flatMap(oauth2User -> {
                    String email = oauth2User.getAttribute("email");
                    String name = oauth2User.getAttribute("name");

                    log.info("회원가입 시도: 이메일={}, 이름={}", email, name);

                    Email userEmail = Email.of(email);

                    return userService.existsByEmail(userEmail)
                            .flatMap(userExists -> {
                                if (userExists) {
                                    // 이미 가입된 사용자 - 로그인 처리
                                    log.info("이미 가입된 사용자 발견: {}", email);
                                    return processLogin(exchange, userEmail);
                                } else {
                                    // 신규 사용자 생성
                                    return processSignup(exchange, userEmail, name);
                                }
                            })
                            .map(ResponseEntity::ok)
                            .onErrorResume(error -> {
                                log.error("회원가입 처리 중 오류", error);
                                return Mono.just(ResponseEntity.badRequest()
                                        .body(LoginResponse.fail("회원가입 처리 중 오류가 발생했습니다.")));
                            });
                })
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest()
                        .body(LoginResponse.fail("인증 정보가 없습니다. 다시 로그인해주세요."))));
    }

    @DeleteMapping("/me")
    public Mono<ResponseEntity<Map<String, String>>> deleteAccount(
            @AuthenticationPrincipal OAuth2User oauth2User,
            ServerWebExchange exchange) {

        String email = oauth2User.getAttribute("email");
        Email userEmail = Email.of(email);

        log.info("회원 탈퇴 요청: {}", email);

        return userService.getUserByEmail(userEmail)
                .flatMap(user -> {
                    log.info("탈퇴 처리 시작: 사용자 ID={}, 이메일={}",
                            user.getId().value(), user.getEmail().value());

                    return userService.deleteUser(user.getId());
                })
                .then(clearAuthenticationCookies(exchange))
                .then(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, createLogoutCookie().toString())
                        .body(Map.of(
                                "success", "true",
                                "message", "회원 탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다.",
                                "timestamp", String.valueOf(System.currentTimeMillis())
                        ))))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.badRequest().body(Map.of(
                                "success", "false",
                                "message", "탈퇴 처리 중 오류가 발생했습니다"
                        )))
                .onErrorReturn(IllegalStateException.class,
                        ResponseEntity.badRequest().body(Map.of(
                                "success", "false",
                                "message", "관리자는 탈퇴할 수 없습니다"
                        )))
                .doOnSuccess(response -> log.info("회원 탈퇴 완료: {}", email))
                .doOnError(error -> log.error("회원 탈퇴 실패: {}, 오류: {}", email, error.getMessage()));
    }

    @PostMapping("/me/withdrawal-request")
    public Mono<ResponseEntity<Map<String, String>>> requestWithdrawal(
            @RequestBody WithdrawalRequestDto request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        String reason = request.reason();

        log.info("탈퇴 사유 수집: 이메일={}, 사유={}", email, reason);

        // 탈퇴 사유 로깅 또는 통계 수집
        return Mono.just(ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "탈퇴 요청이 접수되었습니다. DELETE /api/v1/users/me 를 호출하여 최종 탈퇴하세요.",
                "confirmationRequired", "true",
                "nextStep", "DELETE /api/v1/users/me"
        )));
    }

    private Mono<Void> clearAuthenticationCookies(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> {
                    log.info("세션 무효화: {}", session.getId());
                    session.invalidate();
                    return Mono.empty();
                })
                .then();
    }

    private Mono<LoginResponse> processLogin(ServerWebExchange exchange, Email userEmail) {
        return userService.getUserByEmail(userEmail)
                .map(user -> {
                    String token = jwtTokenProvider.createToken(
                            user.getId().value().toString(),
                            user.getEmail().value(),
                            user.getNickname()
                    );

                    setSecureCookie(exchange.getResponse(), "auth-token", token);

                    return LoginResponse.success(
                            null, // 쿠키로만 전달
                            toUserResponse(user),
                            false // 기존 사용자
                    );
                });
    }

    private Mono<LoginResponse> processSignup(ServerWebExchange exchange, Email userEmail, String name) {
        return userService.createUser(userEmail, name)
                .map(newUser -> {
                    log.info("회원가입 성공: {}", newUser.getEmail().value());

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
                    log.warn("중복 키 에러, 기존 사용자로 처리: {}", userEmail.value());
                    return processLogin(exchange, userEmail);
                });
    }


    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(ServerWebExchange exchange) {
        // 쿠키에서 토큰 추출
        return extractTokenFromCookie(exchange)
                .flatMap(token -> {
                    if (!jwtTokenProvider.validateToken(token)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."));
                    }

                    String email = jwtTokenProvider.getEmail(token);
                    Email userEmail = Email.of(email);

                    log.info("현재 사용자 정보 조회: {}", email);

                    return userService.getUserByEmail(userEmail)
                            .map(this::toUserResponse);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다.")));
    }


    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(ServerWebExchange exchange) {
        log.info("=== 로그아웃 요청 처리 시작 ===");

        // 현재 요청의 쿠키 상태 확인
        var request = exchange.getRequest();
        var cookies = request.getCookies();

        log.info("🍪 로그아웃 전 모든 쿠키: {}", cookies.keySet());

        // auth-token 쿠키 확인
        var authTokenCookies = cookies.get("auth-token");
        if (authTokenCookies != null && !authTokenCookies.isEmpty()) {
            String tokenValue = authTokenCookies.get(0).getValue();
            log.info("🔑 auth-token 존재: {}...", tokenValue.substring(0, Math.min(20, tokenValue.length())));

            // JWT 토큰 정보 로깅
            if (jwtTokenProvider.validateToken(tokenValue)) {
                jwtTokenProvider.logTokenInfo(tokenValue);
            }
        } else {
            log.warn("⚠️ auth-token 쿠키가 없습니다!");
        }

        // 여러 방식으로 auth-token 쿠키 삭제 시도
        ServerHttpResponse response = exchange.getResponse();

        // 방법 1: 기본 경로
        deleteAuthTokenCookie(response, "/");

        // 방법 2: 다양한 경로들
        String[] paths = {"/", "/api", "/oauth2", "/auth"};
        for (String path : paths) {
            deleteAuthTokenCookie(response, path);
        }

        // 방법 3: 도메인별로도 시도
        String[] domains = {"localhost", ".localhost", "127.0.0.1"};
        for (String domain : domains) {
            deleteAuthTokenCookieWithDomain(response, "/", domain);
        }

        return exchange.getSession()
                .flatMap(webSession -> {
                    log.info("📋 세션 ID: {}", webSession.getId());
                    webSession.invalidate();
                    log.info("✅ 세션 무효화 완료");

                    return Mono.just(ResponseEntity.ok(Map.of(
                            "message", "로그아웃 성공",
                            "status", "success",
                            "timestamp", String.valueOf(System.currentTimeMillis())
                    )));
                })
                .contextWrite(ReactiveSecurityContextHolder.clearContext())
                .doOnSuccess(result -> log.info("=== 로그아웃 처리 완료 ==="));
    }

    @GetMapping("/csrf-token")
    public Mono<ResponseEntity<Map<String, String>>> getCsrf(@ModelAttribute Mono<CsrfToken> csrfToken) {
        log.info("csrf token request received");
        return csrfToken
                .map(token -> {
                    log.info(" CSRF 토큰: {}...",
                            token.getToken());

                    Map<String, String> response = new HashMap<>();
                    response.put("token", token.getToken());
                    response.put("headerName", token.getHeaderName());
                    response.put("parameterName", token.getParameterName());

                    return ResponseEntity.ok(response);
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("⚠️ CSRF 토큰을 찾을 수 없음");
                    Map<String, String> response = new HashMap<>();
                    response.put("error", "CSRF token not available");
                    return ResponseEntity.ok(response);
                }));
    }

    private void deleteAuthTokenCookie(ServerHttpResponse response, String path) {
        // ✅ 원본과 정확히 일치하는 설정으로 삭제
        String cookieValue1 = String.format(
                "auth-token=; HttpOnly; SameSite=None; Max-Age=0; Path=%s",
                path
        );
        response.getHeaders().add("Set-Cookie", cookieValue1);

        // ✅ 혹시 모를 경우를 대비한 Secure 없는 버전
        String cookieValue2 = String.format(
                "auth-token=; HttpOnly; SameSite=None; Max-Age=0; Path=%s",
                path
        );
        response.getHeaders().add("Set-Cookie", cookieValue2);

        // ✅ 일반 쿠키도 삭제
        String cookieValue3 = String.format(
                "auth-token=; Max-Age=0; Path=%s",
                path
        );
        response.getHeaders().add("Set-Cookie", cookieValue3);

        log.info("🗑️ 모든 방식으로 쿠키 삭제 시도: Path={}", path);
    }

    private void deleteAuthTokenCookieWithDomain(ServerHttpResponse response, String path, String domain) {
        String cookieValue = String.format(
                "auth-token=; HttpOnly; SameSite=None; Max-Age=0; Path=%s; Domain=%s",
                path, domain
        );
        response.getHeaders().add("Set-Cookie", cookieValue);
        log.info("🗑️ 도메인 포함 쿠키 삭제: Path={}, Domain={}", path, domain);
    }

    private Mono<String> extractTokenFromCookie(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            var cookies = exchange.getRequest().getCookies().get("auth-token");
            if (cookies != null && !cookies.isEmpty()) {
                return cookies.get(0).getValue();
            }
            return null;
        });
    }

    private void setSecureCookie(ServerHttpResponse response, String name, String value) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true) // 이 부분을 true로 설정 (매우 중요)
                .sameSite("None") // 이 부분을 "None"으로 설정 (매우 중요)
                .maxAge(Duration.ofDays(7)) // Max-Age를 Duration으로 설정
                .path("/")
                .domain("duckdns.org") // 이 부분을 반드시 추가하고 'duckdns.org'로 설정
                .build();
        response.addCookie(cookie);
    }

    private void deleteSecureCookie(ServerHttpResponse response, String name) {
        String[] paths = {"/", "/api", "/oauth2"};

        for (String path : paths) {
            String cookieValue = String.format(
                    "%s=; HttpOnly; SameSite=None; Max-Age=0; Path=%s",
                    name, path
            );
            response.getHeaders().add("Set-Cookie", cookieValue);
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.from(user);
    }

    private ResponseCookie createLogoutCookie() {
        return ResponseCookie.from("auth-token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(Duration.ZERO) // 즉시 만료
                .path("/")
                .build();
    }

    public record WithdrawalRequestDto(
            String reason,          // 탈퇴 사유
            boolean dataDelete,     // 데이터 삭제 동의
            String feedback         // 피드백 (선택)
    ) {
    }
}