// 수정된 OAuth2JwtAuthenticationSuccessHandler.java

package com.smwu.matchalot.web.config;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2JwtAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final UserService userService;
    private final String FRONTEND_URL = "http://localhost:3000"; // 환경 변수로 설정 가능

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        log.info("OAuth2 인증 성공: 이메일={}, 이름={}", email, name);

        if (email == null || !email.endsWith("@sookmyung.ac.kr")) {
            log.warn("허용되지 않은 도메인: {}", email);
            return redirectToError(exchange, "domain_not_allowed", "@sookmyung.ac.kr 이메일만 가능해요!");
        }

        Email userEmail = Email.of(email);

        return userService.getOrCreateUser(userEmail, name)
                .flatMap(user -> {
                    log.info("사용자 처리 성공: 이메일={}, 기존 사용자={}", email, user.getCreatedAt() != null);
                    String token = generateJwtToken(user);
                    boolean isNewUser = user.getCreatedAt().equals(user.getCreatedAt()); // 신규 사용자 판단
                    return redirectToSuccess(exchange, token, toUserResponse(user), isNewUser);
                })
                .onErrorResume(error -> {
                    log.error("OAuth2 인증 처리 중 오류: {}", error.getMessage());
                    String errorCode = error instanceof org.springframework.dao.DuplicateKeyException ?
                            "duplicate_user" : "server_error";
                    String errorMessage = error instanceof org.springframework.dao.DuplicateKeyException ?
                            "이미 가입된 사용자입니다." : "서버 오류가 발생했습니다.";
                    return redirectToError(exchange, errorCode, errorMessage);
                });
    }

    private Mono<Void> redirectToSuccess(ServerWebExchange exchange, String token, UserResponse user, boolean isNewUser) {
        String redirectUrl = FRONTEND_URL + "/auth/callback?success=true";
        HttpHeaders headers = new HttpHeaders(); // 새로운 HttpHeaders 객체 생성
        headers.setLocation(URI.create(redirectUrl));
        headers.add("Authorization", "Bearer " + token);
        headers.add("X-User-Id", String.valueOf(user.Id()));
        headers.add("X-Is-New-User", String.valueOf(isNewUser));

        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        exchange.getResponse().getHeaders().addAll(headers);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> redirectToError(ServerWebExchange exchange, String errorCode, String message) {
        String redirectUrl = FRONTEND_URL + "/login?error=" + errorCode + "&message=" + encodeMessage(message);
        HttpHeaders headers = new HttpHeaders(); // 새로운 HttpHeaders 객체 생성
        headers.setLocation(URI.create(redirectUrl));
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        exchange.getResponse().getHeaders().addAll(headers);
        return exchange.getResponse().setComplete();
    }

    private String encodeMessage(String message) {
        try {
            return java.net.URLEncoder.encode(message, "UTF-8");
        } catch (Exception e) {
            log.error("메시지 인코딩 오류: {}", e.getMessage());
            return "알+수+없는+오류가+발생했습니다.";
        }
    }

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