package com.smwu.matchalot.web.controller;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.config.JwtTokenProvider;
import com.smwu.matchalot.web.dto.LoginResponse;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * OAuth2 인증 후 사용자 상태 확인
     * Success Handler에서 리다이렉트된 요청을 처리
     */
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

    /**
     * 신규 사용자 회원가입
     * OAuth2 인증 후 세션에 저장된 정보 사용
     */
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

    private void deleteAuthTokenCookie(ServerHttpResponse response, String path) {
        // HttpOnly 쿠키 삭제
        String cookieValue1 = String.format(
                "auth-token=; HttpOnly; Secure; SameSite=Strict; Max-Age=0; Path=%s",
                path
        );
        response.getHeaders().add("Set-Cookie", cookieValue1);
        log.info("🗑️ HttpOnly 쿠키 삭제 설정: Path={}", path);

        // 일반 쿠키로도 삭제 시도 (HttpOnly 없이)
        String cookieValue2 = String.format(
                "auth-token=; Max-Age=0; Path=%s",
                path
        );
        response.getHeaders().add("Set-Cookie", cookieValue2);
        log.info("🗑️ 일반 쿠키 삭제 설정: Path={}", path);

        // Secure 없이도 시도 (개발환경 대응)
        String cookieValue3 = String.format(
                "auth-token=; HttpOnly; SameSite=Strict; Max-Age=0; Path=%s",
                path
        );
        response.getHeaders().add("Set-Cookie", cookieValue3);
        log.info("🗑️ Secure 없는 쿠키 삭제 설정: Path={}", path);
    }

    private void deleteAuthTokenCookieWithDomain(ServerHttpResponse response, String path, String domain) {
        String cookieValue = String.format(
                "auth-token=; HttpOnly; Secure; SameSite=Strict; Max-Age=0; Path=%s; Domain=%s",
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
        String cookieValue = String.format(
                "%s=%s; HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/",
                name, value
        );
        response.getHeaders().add("Set-Cookie", cookieValue);
    }

    private void deleteSecureCookie(ServerHttpResponse response, String name) {
        String[] paths = {"/", "/api", "/oauth2"};

        for (String path : paths) {
            String cookieValue = String.format(
                    "%s=; HttpOnly; Secure; SameSite=Strict; Max-Age=0; Path=%s",
                    name, path
            );
            response.getHeaders().add("Set-Cookie", cookieValue);
        }
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