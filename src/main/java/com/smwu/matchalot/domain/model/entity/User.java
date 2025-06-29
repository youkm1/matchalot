package com.smwu.matchalot.domain.model.entity;

import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.TrustScore;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.model.vo.UserRole;
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
    private final UserRole role;
    private final LocalDateTime createdAt;

    //new mem
    public User(Email email, String nickname) {
        this(null, email, nickname, TrustScore.DEFAULT, UserRole.PENDING,LocalDateTime.now());
    }

    public User(Email email, String nickname, UserRole role) {
        this(null, email, nickname, TrustScore.DEFAULT, role, LocalDateTime.now());
    }

    public boolean participableInMatch() {
        return trustScore.isAboveThreshold() && (role == UserRole.MEMBER ||role == UserRole.ADMIN);
    }


    public User increaseTrustScoreForGoodMatch() {
        return new User(id, email, nickname, trustScore.increaseMost(), role, createdAt);
    }

    public User decreaseTrustScoreForBadMatch() {
        return new User(id, email, nickname, trustScore.decreaseMost(),role, createdAt);
    }

    private void validateTrustScore(TrustScore newScore) {
        if (newScore == null) {
            throw new IllegalArgumentException("신뢰도는 무족권");
        }
    }

    public User promoteToMember() {
        if (role != UserRole.PENDING) {
            throw new IllegalArgumentException("준회원만 등업 가능합니다");
        }
        int promoScore = TrustScore.MAX;

        return new User(id, email, nickname, new TrustScore(promoScore), UserRole.MEMBER, createdAt);
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isPending() {
        return role == UserRole.PENDING;
    }

    public boolean isMember() {
        return role == UserRole.MEMBER;
    }

    public boolean canUploadMaterial() {
        return role.canUploadMaterial();
    }

    public boolean canRequestMatch() {
        return role.canRequestMatch();
    }
}
