package com.smwu.matchalot.domain.model.vo;

public enum ReportStatus {
    PENDING("접수"),
    RESOLVED("해결완료"),
    REJECTED("기각");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == PENDING;
    }

    public boolean isFinished() {
        return this == RESOLVED || this == REJECTED;
    }
}
