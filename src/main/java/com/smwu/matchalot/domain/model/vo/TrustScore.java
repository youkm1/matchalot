package com.smwu.matchalot.domain.model.vo;

public record TrustScore(int value) {
    public static final TrustScore DEFAULT = new TrustScore(0);
    public static final int MAX = 5;
    public static final int MIN = -5;
    public static final int PARTICIPATION_THRESHOLD = 0;

    public TrustScore {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    String.format("신뢰점수는 %d ~ %d", MIN, MAX)
            );
        }
    }

    public boolean isAboveThreshold() {
        return value >= PARTICIPATION_THRESHOLD;
    }

    public TrustScore increaseForGood() {
        return new TrustScore(Math.min(value + 1, MAX));
    }

    public TrustScore decreaseForGood() {
        return new TrustScore(Math.max(value - 1, MIN));
    }
}
