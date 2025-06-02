package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.reposiotry.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Mono<User> getUserByEmail(Email email) {
        return userRepository.findByEmail(email);
    }

    public Mono<User> createUser(Email email, String nickname) {
        return userRepository.existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("이미 회원입니다. 로그인해주세요!"));
                    }
                    User newUser = new User(email, nickname);
                    return userRepository.save(newUser);
                });
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
