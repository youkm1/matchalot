package com.smwu.matchalot.domain.model.vo;

public record Email(String value) {
    public Email {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일는 무족권");
        }

        if (!value.endsWith("@sookmyung.ac.kr")) {
            throw new IllegalArgumentException("이메일는 숙명대학교 이메일만 사용 가능합니다.");
        }
    }

    public static Email of(String email) {
        return new Email(email);
    }

    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }
}
