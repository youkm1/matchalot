package com.smwu.matchalot.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Matchalot API",
        version = "v1.0.0",
        description = "숙명여대 족보 매칭 서비스 API 명세서",
        contact = @Contact(
            name = "Matchalot Team",
            email = "youkm0806@sookmyung.ac.kr"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development Server"),
        @Server(url = "https://api.matchalot.com", description = "Production Server")
    }
)
@SecurityScheme(
    name = "cookieAuth",
    type = SecuritySchemeType.APIKEY,
    paramName = "auth-token",
    in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE,
    description = "JWT token stored in HttpOnly cookie"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components())
            .addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/api/v1/auth/**")
            .displayName("인증 API")
            .build();
    }

    @Bean
    public GroupedOpenApi studyMaterialApi() {
        return GroupedOpenApi.builder()
            .group("study-materials")
            .pathsToMatch("/api/v1/study-materials/**")
            .displayName("족보 관리 API")
            .build();
    }

    @Bean
    public GroupedOpenApi matchApi() {
        return GroupedOpenApi.builder()
            .group("match")
            .pathsToMatch("/api/v1/match/**")
            .displayName("매칭 API")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/v1/admin/**")
            .displayName("관리자 API")
            .build();
    }

    @Bean
    public GroupedOpenApi reportApi() {
        return GroupedOpenApi.builder()
            .group("report")
            .pathsToMatch("/api/v1/reports/**")
            .displayName("신고 API")
            .build();
    }

    @Bean
    public GroupedOpenApi websocketApi() {
        return GroupedOpenApi.builder()
            .group("websocket")
            .pathsToMatch("/ws/**")
            .displayName("WebSocket API")
            .build();
    }
}