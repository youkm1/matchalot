package com.smwu.matchalot.domain.model.vo;

public record MatchId(Long value) {
    public MatchId {
        if (value == null) {
            throw new IllegalArgumentException("MatchId는 무족권");
        }
    }
    public static MatchId of(Long id) {
        return new MatchId(id);
    }
}
