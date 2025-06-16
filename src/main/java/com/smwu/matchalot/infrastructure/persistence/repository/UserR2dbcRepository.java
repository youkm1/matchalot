package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.infrastructure.persistence.UserEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserR2dbcRepository extends R2dbcRepository<UserEntity, Long> {
    Mono<UserEntity> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);
}
