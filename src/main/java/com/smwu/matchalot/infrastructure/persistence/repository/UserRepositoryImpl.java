package com.smwu.matchalot.infrastructure.persistence.repository;

import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.model.vo.UserRole;
import com.smwu.matchalot.domain.reposiotry.UserRepository;
import com.smwu.matchalot.infrastructure.persistence.UserEntity;
import com.smwu.matchalot.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserR2dbcRepository r2dbcRepository;
    private final UserMapper userMapper;

    public UserRepositoryImpl(UserR2dbcRepository r2dbcRepository, UserMapper userMapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Mono<User> save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        return r2dbcRepository.save(entity)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<User> findById(UserId id) {
        return r2dbcRepository.findById(id.value())
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<User> findByEmail(Email email) {
        return r2dbcRepository.findByEmail(email.value())
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(Email email) {
        return r2dbcRepository.existsByEmail(email.value());
    }

    @Override
    public Mono<Void> deleteById(UserId id) {
        return r2dbcRepository.deleteById(id.value());
    }

    @Override
    public Mono<Long> countByRole(UserRole role) {
        return r2dbcRepository.countByRole(role.name());
    }


    public Flux<User> findAll() {
        return r2dbcRepository.findAllByOrderByCreatedAtDesc()
                .map(userMapper::toDomain);
    }


    public Flux<User> findByRole(UserRole role) {
        return r2dbcRepository.findByRole(role.name())
                .map(userMapper::toDomain);
    }

}