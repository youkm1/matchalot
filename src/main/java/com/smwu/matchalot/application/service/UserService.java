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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Value("${spring.application.email}")
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

    // UserService.java에 추가할 deleteUser 메서드들

    /**
     * 사용자 삭제 (탈퇴)
     */
    public Mono<Void> deleteUser(UserId userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> {
                    log.info("사용자 탈퇴 처리: ID={}, 이메일={}",
                            user.getId().value(), user.getEmail().value());

                    // 탈퇴 전 검증 로직 (필요시)
                    if (user.isAdmin()) {
                        return Mono.error(new IllegalStateException("관리자는 탈퇴할 수 없습니다"));
                    }

                    return userRepository.deleteById(userId);
                })
                .doOnSuccess(ignored -> log.info("사용자 탈퇴 완료: ID={}", userId.value()))
                .doOnError(error -> log.error("사용자 탈퇴 실패: ID={}, 오류={}", userId.value(), error.getMessage()));
    }

    /**
     * 이메일로 사용자 삭제 (편의 메서드)
     */
    public Mono<Void> deleteUserByEmail(Email email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> deleteUser(user.getId()));
    }

    /**
     * 관리자용: 사용자 강제 탈퇴 (신고 처리 등)
     */
    public Mono<Void> forceDeleteUser(UserId userId, String reason) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> {
                    log.warn("관리자에 의한 사용자 강제 탈퇴: ID={}, 이메일={}, 사유={}",
                            user.getId().value(), user.getEmail().value(), reason);

                    // 관리자는 강제 탈퇴 불가
                    if (user.isAdmin()) {
                        return Mono.error(new IllegalStateException("관리자는 강제 탈퇴시킬 수 없습니다"));
                    }

                    return userRepository.deleteById(userId);
                })
                .doOnSuccess(ignored -> log.info("사용자 강제 탈퇴 완료: ID={}, 사유={}", userId.value(), reason))
                .doOnError(error -> log.error("사용자 강제 탈퇴 실패: ID={}, 오류={}", userId.value(), error.getMessage()));
    }

    /**
     * 탈퇴 전 사용자 데이터 정리 (CASCADE 삭제가 안되는 경우)
     */
    public Mono<Void> deleteUserWithCleanup(UserId userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> {
                    log.info("사용자 데이터 정리 후 탈퇴 처리: ID={}", user.getId().value());

                    // 1. 업로드한 족보들 처리 (상태 변경 또는 삭제)
                    // 2. 진행 중인 매칭들 처리
                    // 3. 신고 관련 데이터 처리
                    // TODO: 관련 서비스들과 연동하여 데이터 정리

                    return userRepository.deleteById(userId);
                })
                .doOnSuccess(ignored -> log.info("사용자 데이터 정리 및 탈퇴 완료: ID={}", userId.value()));
    }

    public Flux<User> getAll(UserRole role) {
        if (role != null) {
            return userRepository.findByRole(role);
        } else {
            return userRepository.findAll();
        }
    }
}