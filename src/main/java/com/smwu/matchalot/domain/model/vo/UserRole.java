package com.smwu.matchalot.domain.model.vo;


public enum UserRole {
    PENDING("준회원"), //족보 1개 업로드 대기 상태로 PENDING
    MEMBER("정회원"),
    ADMIN("관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canUploadMaterial() {
        return true; //ban당한 유저는 false
    }

    public boolean canRequestMatch() {
        return this == ADMIN || this == MEMBER;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isMember() {
        return this == MEMBER;
    }

    public boolean isPending() {
        return this == PENDING;
    }
}
