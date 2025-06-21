package com.smwu.matchalot.web.config;

import com.smwu.matchalot.web.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Authorization 헤더에서 토큰 확인 (기존 방식 호환)
        String headerToken = extractTokenFromHeader(request);

        // 2. 쿠키에서 토큰 확인 (새로운 방식)
        String cookieToken = extractTokenFromCookie(request);

        // 우선순위: 헤더 토큰 > 쿠키 토큰
        String token = headerToken != null ? headerToken : cookieToken;

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                String email = jwtTokenProvider.getEmail(token);

                log.debug("인증 성공: userId={}, email={}, source={}",
                        userId, email, headerToken != null ? "header" : "cookie");

                // SecurityContext에 인증 정보 설정
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.emptyList());

                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

            } catch (Exception e) {
                log.warn("토큰 처리 중 오류: {}", e.getMessage());
            }
        }

        return chain.filter(exchange);
    }

    private String extractTokenFromHeader(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private String extractTokenFromCookie(ServerHttpRequest request) {
        List<HttpCookie> cookies = request.getCookies().get("auth-token");
        return cookies != null && !cookies.isEmpty() ? cookies.get(0).getValue() : null;
    }
}