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

    public static final Subject COMPUTER_NETWORK_1 = new Subject("컴퓨터네트워크Ⅰ");
    public static final Subject SOCIAL_MEDIA_UNDERSTANDING = new Subject("소셜미디어의이해와활용");
    public static final Subject KOREAN_CULTURE_UNDERSTANDING = new Subject("한국문화의이해");
    public static final Subject CLASSIC_FIELD_STORY = new Subject("고전의현장과스토리");
    public static final Subject DIGITAL_PHILOSOPHY = new Subject("디지털철학");
    public static final Subject WESTERN_HISTORY_CULTURE = new Subject("서양의역사와문화");
    public static final Subject COMPUTER_MATH_YO = new Subject("컴퓨터수학-최영우 교수님");
    public static final Subject COMPUTER_MATH_HJ = new Subject("컴퓨터수학-채희준 교수님");


}
