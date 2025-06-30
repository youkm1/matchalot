package com.smwu.matchalot.domain.model.vo;

public record ExamType(String type) {
    public static final ExamType MIDTERM = new ExamType("MIDTERM");
    public static final ExamType FINAL = new ExamType("FINAL");

    public ExamType {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("시험 유형은 필수입니다.");
        }
    }

    public static ExamType of(String type) {
        return new ExamType(type);
    }
}