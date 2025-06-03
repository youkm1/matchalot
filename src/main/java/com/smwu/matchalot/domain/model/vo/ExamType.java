package com.smwu.matchalot.domain.model.vo;

public record ExamType(String type) {
    public static final ExamType MIDTERM = new ExamType("중간고사");
    public static final ExamType FINAL = new ExamType("기말고사");

    public ExamType {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("셤 유형은 필수입니다.");
        }
    }

    public static ExamType of(String type) {
        return new ExamType(type);
    }
}
