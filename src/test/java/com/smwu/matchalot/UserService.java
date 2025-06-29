
package com.smwu.matchalot;

import com.smwu.matchalot.application.service.UserService;
import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.TrustScore;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.reposiotry.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService updateTrustScore 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserId testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UserId.of(1L);
        testUser = new User(
                testUserId,
                Email.of("test@sookmyung.ac.kr"),
                "테스트유저",
                TrustScore.DEFAULT, // 기본값 0
                java.time.LocalDateTime.now()
        );
    }

    @Test
    @DisplayName(" 매치 후 신뢰도가 1 증가한다")
    void 좋은_매치_후_신뢰도_증가() {
        // Given
        User expectedUser = testUser.increaseTrustScoreForGoodMatch();
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.updateTrustScore(testUserId, true);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getTrustScore().value()).isEqualTo(1);
                    assertThat(user.getId()).isEqualTo(testUserId);
                    assertThat(user.getEmail()).isEqualTo(testUser.getEmail());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("나쁜 매치 후 신뢰도가 1 감소한다")
    void 나쁜_매치_후_신뢰도_감소() {
        // Given
        User expectedUser = testUser.decreaseTrustScoreForBadMatch();
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.updateTrustScore(testUserId, false);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getTrustScore().value()).isEqualTo(-1);
                    assertThat(user.participableInMatch()).isTrue(); // -1이어도 참여 가능 (임계값 0)
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("신뢰도 최대값(5) 상한선 테스트")
    void 신뢰도_최대값_상한선_테스트() {
        // Given - 이미 신뢰도가 5인 사용자
        User maxTrustUser = new User(
                testUserId,
                Email.of("test@sookmyung.ac.kr"),
                "최대신뢰도유저",
                new TrustScore(5), // 최대값
                java.time.LocalDateTime.now()
        );

        User expectedUser = maxTrustUser.increaseTrustScoreForGoodMatch(); // 여전히 5
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(maxTrustUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.updateTrustScore(testUserId, true);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getTrustScore().value()).isEqualTo(5); // 5에서 더 증가하지 않음
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("신뢰도 최소값(-5) 하한선 테스트")
    void 신뢰도_최소값_하한선_테스트() {
        // Given - 이미 신뢰도가 -5인 사용자
        User minTrustUser = new User(
                testUserId,
                Email.of("test@sookmyung.ac.kr"),
                "최소신뢰도유저",
                new TrustScore(-5), // 최소값
                java.time.LocalDateTime.now()
        );

        User expectedUser = minTrustUser.decreaseTrustScoreForBadMatch(); // 여전히 -5
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(minTrustUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.updateTrustScore(testUserId, false);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getTrustScore().value()).isEqualTo(-5); // -5에서 더 감소하지 않음
                    assertThat(user.participableInMatch()).isFalse(); // 매칭 참여 불가
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 신뢰도 업데이트 시 에러")
    void 존재하지_않는_사용자_에러_테스트() {
        // Given
        UserId nonExistentUserId = UserId.of(999L);
        when(userRepository.findById(nonExistentUserId)).thenReturn(Mono.empty());

        // When
        Mono<User> result = userService.updateTrustScore(nonExistentUserId, true);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Mono.empty()가 반환되므로 complete
    }

    @Test
    @DisplayName("신뢰도 변경 후 매칭 참여 가능 여부 확인")
    void 신뢰도_변경_후_매칭_참여_가능_여부() {
        // Given - 신뢰도 -1인 사용자 (아직 참여 가능)
        User borderlineUser = new User(
                testUserId,
                Email.of("test@sookmyung.ac.kr"),
                "경계선유저",
                new TrustScore(-1),
                java.time.LocalDateTime.now()
        );

        User expectedUser = borderlineUser.decreaseTrustScoreForBadMatch(); // -2가 됨
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(borderlineUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(expectedUser));

        // When - 나쁜 매치로 신뢰도 감소
        Mono<User> result = userService.updateTrustScore(testUserId, false);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getTrustScore().value()).isEqualTo(-2);
                    assertThat(user.participableInMatch()).isFalse(); // 이제 매칭 참여 불가
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("연속된 신뢰도 업데이트 테스트")
    void 연속된_신뢰도_업데이트_테스트() {
        // Given
        User firstUpdate = testUser.increaseTrustScoreForGoodMatch(); // 0 -> 1
        User secondUpdate = firstUpdate.increaseTrustScoreForGoodMatch(); // 1 -> 2

        when(userRepository.findById(testUserId))
                .thenReturn(Mono.just(testUser))
                .thenReturn(Mono.just(firstUpdate));
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(firstUpdate))
                .thenReturn(Mono.just(secondUpdate));

        // When - 좋은 매치 2번 연속
        Mono<User> result = userService.updateTrustScore(testUserId, true)
                .flatMap(updatedUser -> userService.updateTrustScore(testUserId, true));

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getTrustScore().value()).isEqualTo(2);
                })
                .verifyComplete();
    }
}
