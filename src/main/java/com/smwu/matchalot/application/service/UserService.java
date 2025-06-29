package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.model.vo.UserRole;
import com.smwu.matchalot.domain.reposiotry.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Value("${matchalot.admin.emails}")
    private String ADMIN_EMAILS;

    public Mono<User> getUserByEmail(Email email) {
        return userRepository.findByEmail(email);
    }


    public Mono<User> createUser(Email email, String nickname) {
        log.info("새 사용자 생성 시도: 이메일={}, 닉네임={}", email.value(), nickname);
        // 메서드 없이 바로 사용
        UserRole role = Set.of(ADMIN_EMAILS.split(",")).contains(email.value()) ? UserRole.ADMIN : UserRole.PENDING;
        User newUser = new User(email, nickname, role);
        return userRepository.save(newUser)
                .doOnSuccess(saved -> log.info("사용자 생성 완료: ID={}, 이메일={}",
                        saved.getId() != null ? saved.getId().value() : "null",
                        saved.getEmail().value(),
                        saved.getRole().getDescription()))
                .doOnError(error -> log.error("사용자 생성 실패: 이메일={}, 오류={}", email.value(), error.getMessage()));
    }



    public Mono<Boolean> existsByEmail(Email email) {
        return userRepository.existsByEmail(email);
    }

    public Mono<User> updateTrustScore(UserId userId, boolean isGoodMatch) {
        return userRepository.findById(userId)
                .map(user -> isGoodMatch ?
                        user.increaseTrustScoreForGoodMatch() :
                        user.decreaseTrustScoreForBadMatch())
                .flatMap(userRepository::save);
    }

    public Mono<User> getUserById(UserId userId) {
        return userRepository.findById(userId);
    }

    //등업
    public Mono<User> promoteToMember(UserId userId) {
        return userRepository.findById(userId)
                .map(User::promoteToMember)
                .flatMap(userRepository::save)
                .doOnSuccess(user -> log.info("등업 성공:{} {}", user.getId().value(), user.getRole().getDescription()));
    }

    public Mono<Boolean> isAdminById(UserId userId) {
        return userRepository.findById(userId)
                .map(User::isAdmin)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isAdminByEmail(Email email) {
        return userRepository.findByEmail(email)
                .map(User::isAdmin)
                .defaultIfEmpty(Set.of(ADMIN_EMAILS.split(",")).contains(email.value()));
    }

    public Mono<Long> countByRole(UserRole role) {
        return userRepository.countByRole(role);
    }
}