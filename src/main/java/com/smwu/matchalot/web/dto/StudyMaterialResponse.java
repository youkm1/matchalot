package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.StudyMaterial;

import java.time.LocalDateTime;
import java.util.List;

public record StudyMaterialResponse(
        Long id,
        String subject,
        String examType,
        Integer year,
        String season,
        String semesterDisplay,
        String title,
        Integer questionCount,
        List<QuestionResponse> questions,  // 모든 문제들
        String uploaderNickname,
        LocalDateTime createdAt,
        String displayTitle
) {
    // 도메인 엔티티로부터 DTO 생성하는 정적 팩토리 메서드
    public static StudyMaterialResponse from(StudyMaterial studyMaterial, String uploaderNickname) {
        List<QuestionResponse> questionResponses = studyMaterial.getAllQuestions().stream()
                .map(QuestionResponse::from)
                .toList();

        return new StudyMaterialResponse(
                studyMaterial.getId() != null ? studyMaterial.getId().value() : null,
                studyMaterial.getSubject().name(),
                studyMaterial.getExamType().type(),
                studyMaterial.getSemester().year(),
                studyMaterial.getSemester().season(),
                studyMaterial.getSemester().getDisplayName(),
                studyMaterial.getTitle(),
                studyMaterial.getQuestionCount(),
                questionResponses,
                uploaderNickname,
                studyMaterial.getCreatedAt(),
                studyMaterial.getDisplayTitle()
        );
    }
}