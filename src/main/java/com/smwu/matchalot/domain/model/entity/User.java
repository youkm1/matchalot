package com.smwu.matchalot.domain.model.entity;

import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.TrustScore;
import com.smwu.matchalot.domain.model.vo.UserId;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class User {
    private final UserId id;
    private final Email email;
    private final String nickname;
    private final TrustScore trustScore;
    private final LocalDateTime createdAt;

    //new mem
    public User(Email email, String nickname) {
        this(null, email, nickname, TrustScore.DEFAULT, LocalDateTime.now());
    }

    public boolean participableInMatch() {
        return trustScore.isAboveThreshold();
    }

    public User updateTrustScore(TrustScore newScore) {
        validateTrustScore(newScore);
        return new User(id, email, nickname, newScore, createdAt);
    }

    public User increaseeTrustScoreForGoodMatch() {
        return new User(id, email, nickname, trustScore.increaseForGood(), createdAt);
    }

    public User decreaseTrustScoreForBadMatch() {
        return new User(id, email, nickname, trustScore.decreaseForGood(), createdAt);
    }

    private void validateTrustScore(TrustScore newScore) {
        if (newScore == null) {
            throw new IllegalArgumentException("신뢰도는 무족권");
        }
    }
}
