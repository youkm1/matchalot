package com.smwu.matchalot.domain.reposiotry;

import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.model.vo.UserRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<User> save(User user);
    Mono<User> findById(UserId id);
    Mono<User> findByEmail(Email email);
    Mono<Boolean> existsByEmail(Email email);
    Mono<Void> deleteById(UserId id);
    Mono<Long> countByRole(UserRole role);
    Flux<User> findAll();
    Flux<User> findByRole(UserRole role);
}
