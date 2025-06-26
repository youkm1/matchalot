package com.smwu.matchalot.domain.model.vo;

public record StudyMaterialId(Long value) {
    public static StudyMaterialId of(Long id) {
        return new StudyMaterialId(id);
    }

    public boolean isPresent() {
        return value != null;
    }
}
