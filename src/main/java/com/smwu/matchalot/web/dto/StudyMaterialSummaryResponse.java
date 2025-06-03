package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;

import java.time.LocalDateTime;

public record StudyMaterialSummaryResponse(
        Long id,
        String subject,
        String examType,
        String semesterDisplay,
        String title,
        Integer questionCount,
        String uploaderNickname,
        LocalDateTime createdAt
) {
    public static StudyMaterialSummaryResponse from(StudyMaterial studyMaterial, String uploaderNickname) {
        return new StudyMaterialSummaryResponse(
                studyMaterial.getId().value(),
                studyMaterial.getSubject().name(),
                studyMaterial.getExamType().type(),
                studyMaterial.getSemester().getDisplayName(),
                studyMaterial.getTitle(),
                studyMaterial.getQuestionCount(),
                uploaderNickname,
                studyMaterial.getCreatedAt()
        );
    }
}
