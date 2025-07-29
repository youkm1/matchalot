package com.smwu.matchalot.web.config;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2JwtAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Value("${app.frontend.url}")
    private String FRONTEND_URL;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        log.info("OAuth2 인증 성공: 이메일={}, 이름={}", email, name);

        // 도메인 검증
        if (email == null || !email.endsWith("@sookmyung.ac.kr")) {
            log.warn("허용되지 않은 도메인: {}", email);
            return redirectToError(exchange, "domain_not_allowed", "@sookmyung.ac.kr 이메일만 가능해요!");
        }

        Email userEmail = Email.of(email);

        // 세션에 OAuth2User 정보 저장 (AuthController에서 사용할 수 있도록)
        return exchange.getSession()
                .flatMap(webSession -> {
                    webSession.getAttributes().put("oauth2User", oauth2User);
                    log.debug("세션에 OAuth2User 저장: {}", email);

                    // 사용자 존재 여부 확인 후 적절한 처리
                    return userService.existsByEmail(userEmail)
                            .flatMap(userExists -> {
                                if (userExists) {
                                    // 기존 사용자 - 바로 로그인 처리
                                    return processExistingUser(exchange, userEmail, webSession);
                                } else {
                                    // 신규 사용자 - 회원가입 필요
                                    return redirectToSignup(exchange, email, name);
                                }
                            });
                })
                .onErrorResume(error -> {
                    log.error("OAuth2 인증 처리 중 오류: {}", error.getMessage(), error);
                    return redirectToError(exchange, "server_error", "서버 오류가 발생했습니다.");
                });
    }

    private Mono<Void> processExistingUser(ServerWebExchange exchange, Email userEmail, org.springframework.web.server.WebSession webSession) {
        return userService.getUserByEmail(userEmail)
                .flatMap(user -> {
                    // JWT 토큰 생성
                    String token = jwtTokenProvider.createToken(
                            user.getId().value().toString(),
                            user.getEmail().value(),
                            user.getNickname()
                    );
                    log.info("JWT 토큰 생성: {}", user.getEmail().value());
                    // 쿠키에 토큰 저장
                    setSecureCookie(exchange.getResponse(), "auth-token", token);

                    // 세션에 사용자 정보 저장
                    webSession.getAttributes().put("userId", user.getId().value().toString());
                    webSession.getAttributes().put("userEmail", user.getEmail().value());

                    log.info("기존 사용자 로그인 완료: {}", user.getEmail().value());

                    // 로그인 성공으로 리다이렉트
                    return redirectToLoginSuccess(exchange, user, false);
                });
    }

    private Mono<Void> redirectToSignup(ServerWebExchange exchange, String email, String name) {
        String redirectUrl = String.format("%s/auth/callback?action=signup&email=%s&name=%s",
                FRONTEND_URL,
                encodeMessage(email),
                encodeMessage(name));

        return redirect(exchange, redirectUrl);
    }

    private Mono<Void> redirectToLoginSuccess(ServerWebExchange exchange, User user, boolean isNewUser) {
        String redirectUrl = String.format("%s/auth/callback?success=true&isNewUser=%s&userId=%s",
                FRONTEND_URL,
                isNewUser,
                user.getId().value().toString());

        return redirect(exchange, redirectUrl);
    }

    private Mono<Void> redirectToError(ServerWebExchange exchange, String errorCode, String message) {
        log.info("🔧 FRONTEND_URL 사용: {}", FRONTEND_URL);
        String redirectUrl = String.format("%s/login?error=%s&message=%s",
                FRONTEND_URL,
                errorCode,
                encodeMessage(message));
        log.info("🔧 리다이렉트 URL: {}", redirectUrl);
        return redirect(exchange, redirectUrl);
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String url) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(URI.create(url));
        return exchange.getResponse().setComplete();
    }

    public void setSecureCookie(ServerHttpResponse response, String name, String value) {
       ResponseCookie cookie = ResponseCookie.from(name, value)
               .httpOnly(true)
               .secure(true)
               .sameSite("None")
               .maxAge(604800)
               .path("/")
               .domain("matchalot.duckdns.org")
               .build();
       response.addCookie(cookie);
        log.info("Set-Cookie: {} with attributes: HttpOnly={}, Secure={}, SameSite={}, MaxAge={}, Path={}, Domain={}",
                name, cookie.isHttpOnly(), cookie.isSecure(), cookie.getSameSite(),
                cookie.getMaxAge(), cookie.getPath(), cookie.getDomain());
    }

    private String encodeMessage(String message) {
        try {
            return java.net.URLEncoder.encode(message, "UTF-8");
        } catch (Exception e) {
            log.error("메시지 인코딩 오류: {}", e.getMessage());
            return "error";
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.from(user);
    }
}
