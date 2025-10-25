package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.infrastructure.persistence.UserEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserR2dbcRepository extends R2dbcRepository<UserEntity, Long> {
    Mono<UserEntity> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);
    Mono<Long> countByRole(String role);
    Flux<UserEntity> findAllByOrderByCreatedAtDesc();
    Flux<UserEntity> findByRole(String role);
    Flux<UserEntity> findByRoleOrderByCreatedAtDesc(String role);

}
