package com.smwu.matchalot.web.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private final JwtTokenProvider jwtTokenProvider;

    private final OAuth2JwtAuthenticationSuccessHandler oauth2SuccessHandler;

    private final CookieAuthenticationFilter cookieAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(exchange -> {
                    CorsConfiguration config = new CorsConfiguration();

                    config.setAllowedOriginPatterns(Arrays.asList(
                            "http://localhost:3000",
                            "http://127.0.0.1:3000",
                            frontendUrl
                    ));
                    config.setAllowedMethods(Arrays.asList(
                            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
                    ));
                    config.setAllowedHeaders(Arrays.asList("*"));

                    // CSRF 토큰 헤더도 노출 (중요!)
                    config.setExposedHeaders(Arrays.asList(
                            "Access-Control-Allow-Origin",
                            "Access-Control-Allow-Credentials",
                            "X-CSRF-Token", // 추가!
                            "Set-Cookie"    // 추가!
                    ));

                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                }))

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/login", "/oauth2/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )


                .addFilterBefore(cookieAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION)

                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(oauth2SuccessHandler)
                        .authenticationFailureHandler(authenticationFailureHandler())
                )

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(ServerWebExchangeMatchers.matchers(
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.DELETE,"/api/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST,"/api/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.PATCH,"/api/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.PUT,"/api/**")
                        ))
                )
                .build();
    }





    @Bean
    public ServerAuthenticationFailureHandler authenticationFailureHandler() {
        return (exchange, exception) -> {
            var response = exchange.getExchange().getResponse();
            response.setStatusCode(HttpStatus.FOUND);

            try {
                String message = URLEncoder.encode("OAuth2 인증에 실패했습니다.", StandardCharsets.UTF_8);
                String redirectUrl = frontendUrl + "/login?error=oauth_error&message=" + message;
                response.getHeaders().add("Location", redirectUrl);
            } catch (Exception e) {
                response.getHeaders().add("Location", frontendUrl + "/login?error=oauth_error");
            }

            return response.setComplete();
        };
    }
}