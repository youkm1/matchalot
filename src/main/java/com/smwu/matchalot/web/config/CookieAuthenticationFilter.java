package com.smwu.matchalot.web.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();


        if ("/api/v1/auth/logout".equals(path)) {
            log.debug("로그아웃 경로 - 인증 필터 스킵");
            return chain.filter(exchange);
        }

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
                String nickname = jwtTokenProvider.getNickname(token);

                log.debug("인증 성공: userId={}, email={}, nickname={}, source={}",
                        userId, email, nickname, headerToken != null ? "header" : "cookie");

                Map<String, Object> attributes = Map.of(
                        "email", email,
                        "name", nickname,
                        "id", userId
                );
                //<GrantedAuthority>로 역할 기반 sooktin
                OAuth2User oAuth2User = new DefaultOAuth2User(
                        Collections.<GrantedAuthority>emptyList(),
                        attributes,
                        "email"
                );
                // OAuth2AuthenticationToken -> 신분이확인된사람
                OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                        oAuth2User,
                        Collections.<GrantedAuthority>emptyList(),
                        "google"
                );

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
        log.info("Cookies received: {}", request.getCookies());
        String token = cookies != null && !cookies.isEmpty() ? cookies.get(0).getValue() : null;
        log.info("Extracted auth-token: {}", token);
        return token;
    }
}
