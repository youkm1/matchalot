package com.smwu.matchalot.web.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.frontend.url}")
    private String frontendUrl;


    private final OAuth2JwtAuthenticationSuccessHandler oauth2SuccessHandler;

    private final CookieAuthenticationFilter cookieAuthenticationFilter;

    @Bean
    public CookieServerCsrfTokenRepository csrfTokenRepository() {
        CookieServerCsrfTokenRepository repository = CookieServerCsrfTokenRepository.withHttpOnlyFalse();

        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setParameterName("_csrf");
        repository.setCookiePath("/");


        repository.setCookieCustomizer(cookieBuilder ->
                cookieBuilder
                        .domain(".match-a-lot.store")
                        .sameSite("Lax")
        );

        return repository;
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                frontendUrl,
                "https://matchalot.duckdns.org",
                "https://matchalot.vercel.app",
                "https://api.match-a-lot.store",
                "http://localhost:3000",
                "http://localhost:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE","OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("*"));  // SSE를 위한 헤더 노출

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        // SSE 엔드포인트용 특별 설정
        CorsConfiguration sseConfiguration = new CorsConfiguration();
        sseConfiguration.setAllowedOrigins(configuration.getAllowedOrigins());
        sseConfiguration.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
        sseConfiguration.setAllowedHeaders(Arrays.asList("*"));
        sseConfiguration.setAllowCredentials(true);
        sseConfiguration.setMaxAge(3600L);  // preflight 캐시 1시간
        source.registerCorsConfiguration("/api/v1/notifications/stream", sseConfiguration);
        
        return source;
    }
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))


                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/login/**", "/oauth2/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        // Swagger UI 및 API 문서 경로 허용
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .pathMatchers("/api-docs/**", "/api-docs.yaml/**").permitAll()
                        .pathMatchers("/webjars/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/study-materials").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/study-materials/subjects").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/study-materials/exam-types").permitAll()
                        ///api/v1/study-materials/{id}, 족보삭제, 내 자료와 업로드 api부터는 인증 필요
                        
                        // SSE는 인증 필요
                        .pathMatchers("/api/v1/notifications/stream").authenticated()
                        .anyExchange().authenticated()
                )


                .addFilterBefore(cookieAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION)

                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(oauth2SuccessHandler)
                        .authenticationFailureHandler(authenticationFailureHandler())
                )


                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .requireCsrfProtectionMatcher(ServerWebExchangeMatchers.matchers(
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/v1/study-materials/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.PUT, "/api/v1/study-materials/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.DELETE, "/api/v1/study-materials/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.PATCH, "/api/v1/study-materials/**"),

                                // 다른 보호 대상 API들
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/v1/match/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.PUT, "/api/v1/match/**"),
                                ServerWebExchangeMatchers.pathMatchers(HttpMethod.DELETE, "/api/v1/match/**")
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
