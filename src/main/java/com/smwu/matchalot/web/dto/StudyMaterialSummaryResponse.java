package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;

import java.time.LocalDateTime;

public record StudyMaterialSummaryResponse(
        Long id,
        Long uploaderId,
        String subject,
        String examType,
        String semesterDisplay,
        String title,
        Integer questionCount,
        String uploaderNickname,
        Integer uploaderTrustScore,
        LocalDateTime createdAt,
        String tempPdfData
) {
    public static StudyMaterialSummaryResponse from(StudyMaterial studyMaterial, String uploaderNickname, Integer trustScore) {
        return new StudyMaterialSummaryResponse(
                studyMaterial.getId().value(),
                studyMaterial.getUploaderId().value(),
                studyMaterial.getSubject().name(),
                studyMaterial.getExamType().type(),
                studyMaterial.getSemester().getDisplayName(),
                studyMaterial.getTitle(),
                studyMaterial.getQuestionCount(),
                uploaderNickname,
                trustScore,
                studyMaterial.getCreatedAt(),
                studyMaterial.getTempPdfData()
        );
    }
}
