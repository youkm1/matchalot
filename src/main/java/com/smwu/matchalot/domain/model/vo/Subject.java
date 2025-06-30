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

    public static final Subject KOREAN_WOMEN_HISTORY = new Subject("KOREAN_WOMEN_HISTORY");
    public static final Subject ALGORITHM = new Subject("ALGORITHM");
    public static final Subject DIGITAL_LOGIC_CIRCUIT = new Subject("DIGITAL_LOGIC_CIRCUIT");
    public static final Subject STATISTICS_INTRODUCTION = new Subject("STATISTICS_INTRODUCTION");
}
