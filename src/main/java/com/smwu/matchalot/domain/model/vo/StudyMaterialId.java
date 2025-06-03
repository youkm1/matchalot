package com.smwu.matchalot.domain.model.vo;

public record StudyMaterialId(Long value) {
    public StudyMaterialId {
        if (value == null) {
            throw new IllegalArgumentException("족보Id는 필수");
        }
    }
    public static StudyMaterialId of(Long id) {
        return new StudyMaterialId(id);
    }
}
