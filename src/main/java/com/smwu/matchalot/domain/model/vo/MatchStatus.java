package com.smwu.matchalot.domain.model.vo;

public enum MatchStatus {
    PENDING("대기중"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨"),
    COMPLETED("완료됨"),
    EXPIRED("만료됨");

    private final String description;

    MatchStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == PENDING || this == ACCEPTED;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == REJECTED || this == EXPIRED;
    }
}
