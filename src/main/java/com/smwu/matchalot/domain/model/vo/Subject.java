package com.smwu.matchalot.domain.model.vo;

public record Subject(String name) {
    public Subject {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("과목명은 필수입니다.");
        }
    }

    public static Subject of(String name) {
        return new Subject(name);
    }

    public static final Subject PROGRAMMING_LANGUAGES = new Subject("프로그래밍 언어론");
    public static final Subject COMPUTER_ARCHITECTURE = new Subject("캄퓨터구조");
}
