package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.reposiotry.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public Mono<User> getUserByEmail(Email email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 사용자 생성 (존재하지 않을 때만)
     */
    public Mono<User> createUser(Email email, String nickname) {
        log.info("새 사용자 생성 시도: 이메일={}, 닉네임={}", email.value(), nickname);

        User newUser = new User(email, nickname);
        return userRepository.save(newUser)
                .doOnSuccess(saved -> log.info("사용자 생성 완료: ID={}, 이메일={}",
                        saved.getId() != null ? saved.getId().value() : "null", saved.getEmail().value()))
                .doOnError(error -> log.error("사용자 생성 실패: 이메일={}, 오류={}", email.value(), error.getMessage()));
    }

    /**
     * OAuth 로그인/회원가입 통합 처리
     * 기존 사용자면 조회, 신규 사용자면 생성하고 결과 반환
     */
    public Mono<User> getOrCreateUser(Email email, String nickname) {
        log.info("사용자 조회/생성: 이메일={}, 닉네임={}", email.value(), nickname);

        return userRepository.findByEmail(email)
                .doOnSuccess(existingUser -> {
                    if (existingUser != null) {
                        log.info("기존 사용자 조회: {}", existingUser.getEmail().value());
                    }
                })
                .switchIfEmpty(
                        // 신규 사용자 생성
                        createUser(email, nickname)
                                .doOnSuccess(newUser ->
                                        log.info("신규 사용자 생성: {}", newUser.getEmail().value()))
                                .onErrorResume(org.springframework.dao.DuplicateKeyException.class, ex -> {
                                    // 동시 생성 시 중복 에러 발생하면 다시 조회
                                    log.warn("중복 키 에러로 인한 재조회: {}", email.value());
                                    return userRepository.findByEmail(email);
                                })
                );
    }

    /**
     * 이메일로 사용자 존재 여부 확인
     */
    public Mono<Boolean> existsByEmail(Email email) {
        return userRepository.existsByEmail(email);
    }

    public Mono<User> updateTrustScore(UserId userId, boolean isGoodMatch) {
        return userRepository.findById(userId)
                .map(user -> isGoodMatch ?
                        user.increaseeTrustScoreForGoodMatch() :
                        user.decreaseTrustScoreForBadMatch())
                .flatMap(userRepository::save);
    }

    public Mono<User> getUserById(UserId userId) {
        return userRepository.findById(userId);
    }
}