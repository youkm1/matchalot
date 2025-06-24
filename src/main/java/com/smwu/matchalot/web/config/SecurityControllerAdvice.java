package com.smwu.matchalot.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ControllerAdvice
@Slf4j
public class SecurityControllerAdvice {
    @ModelAttribute
    public Mono<CsrfToken> csrfToken(ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
        return csrfToken.doOnSuccess(token -> {
            if (token != null) {
                exchange.getAttributes().put("_csrf", token);
            } else {
                log.warn("CSRF 토큰을 찾을 수 없음");
            }
        });
    }
}
