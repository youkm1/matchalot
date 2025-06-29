package com.smwu.matchalot.domain.model.vo;

public enum MaterialStatus {
    PENDING("승인 대기"),      // 업로드 후 관리자 검토 대기
    APPROVED("승인 완료"),     // 관리자 승인 완료, 매칭 가능
    REJECTED("승인 거절");     // 관리자 거절, 재업로드 필요

    private final String description;

    MaterialStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }

    public boolean canBeUsedForMatching() {
        return this == APPROVED;
    }
}
