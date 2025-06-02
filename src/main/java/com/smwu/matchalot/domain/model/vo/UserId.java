package com.smwu.matchalot.domain.model.vo;

public record UserId(Long value) {
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId는 무족권");
        }
    }

    public static UserId of(Long id) {
        return new UserId(id);
    }
}
