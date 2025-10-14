package com.smwu.matchalot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "com.smwu.matchalot.infrastructure.persistence.repository")
@EnableAsync
public class MatchalotApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MatchalotApplication.class, args);
    }

}
