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
        Integer uploaderTrustScore,
        LocalDateTime createdAt,
        String tempPdfData
) {
    public static StudyMaterialSummaryResponse from(StudyMaterial studyMaterial, Integer trustScore) {
        return new StudyMaterialSummaryResponse(
                studyMaterial.getId().value(),
                studyMaterial.getUploaderId().value(),
                studyMaterial.getSubject().name(),
                studyMaterial.getExamType().type(),
                studyMaterial.getSemester().getDisplayName(),
                studyMaterial.getTitle(),
                studyMaterial.getQuestionCount(),
                trustScore,
                studyMaterial.getCreatedAt(),
                studyMaterial.getTempPdfData()
        );
    }

    // JOIN 쿼리 결과를 직접 매핑하기 위한 생성자
    public static StudyMaterialSummaryResponse fromJoinResult(
            Long id,
            Long uploaderId,
            String subject,
            String examType,
            Integer year,
            String season,
            String title,
            Integer questionCount,
            Integer uploaderTrustScore,
            LocalDateTime createdAt,
            String tempPdfData
    ) {
        String semesterDisplay = year + "년 " + season;
        return new StudyMaterialSummaryResponse(
                id,
                uploaderId,
                subject,
                examType,
                semesterDisplay,
                title,
                questionCount,
                uploaderTrustScore,
                createdAt,
                tempPdfData
        );
    }
}
