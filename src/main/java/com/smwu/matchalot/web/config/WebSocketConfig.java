package com.smwu.matchalot.web.config;

import com.smwu.matchalot.web.websocket.MatchWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final MatchWebSocketHandler matchWebSocketHandler;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public SimpleUrlHandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> urlMap = new HashMap<>();
        urlMap.put("/ws/match", matchWebSocketHandler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(urlMap);
        mapping.setOrder(1);
        
        // CORS 헤더 설정 (WebSocket은 별도 CORS 설정 필요)
        mapping.setCorsConfigurations(getCorsConfigurations());
        
        return mapping;
    }
    
    private Map<String, org.springframework.web.cors.CorsConfiguration> getCorsConfigurations() {
        Map<String, org.springframework.web.cors.CorsConfiguration> corsConfigs = new HashMap<>();
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        
        config.addAllowedOrigin(frontendUrl);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        
        corsConfigs.put("/ws/**", config);
        return corsConfigs;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}