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

    public static final Subject IMAGE_PROCESSING = new Subject("영상정보처리");
    public static final Subject LINUX = new Subject("리눅스");
    public static final Subject KOREAN_CULTURE_UNDERSTANDING = new Subject("한국문화의이해");
    public static final Subject CLASSIC_FIELD_STORY = new Subject("고전의현장과스토리");
    public static final Subject DIGITAL_PHILOSOPHY = new Subject("디지털철학");
}
