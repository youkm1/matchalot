package com.smwu.matchalot.domain.model.vo;

public record ReportId(Long value) {
    public ReportId {
        if (value == null) {
            throw new IllegalArgumentException("신고하며 무조건 id는 생성됨");
        }
    }

    public static ReportId of(Long id) {
        return new ReportId(id);
    }
}
