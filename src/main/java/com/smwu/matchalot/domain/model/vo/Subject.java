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

    public static final Subject KOREAN_WOMEN_HISTORY = new Subject("한국여성의역사");
    public static final Subject ALGORITHM = new Subject("알고리즘");
    public static final Subject DIGITAL_LOGIC_CIRCUIT = new Subject("디지털논리회로");
    public static final Subject MODERN_THOUGH = new Subject("보고듣고만지는현대사상");
}
