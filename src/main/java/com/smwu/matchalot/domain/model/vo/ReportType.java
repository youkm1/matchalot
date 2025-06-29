package com.smwu.matchalot.domain.model.vo;

public enum ReportType {
    POOR_QUALITY("부적절한 내용"),
    FAKE_MATERIAL("가짜 자료"),
    DIFFERENT_MATERIAL("다른 자료");

    private final String description;

    ReportType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
