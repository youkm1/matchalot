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
        Long uploaderId,
        Integer uploaderTrustScore,
        LocalDateTime createdAt,
        String displayTitle
) {
    // 도메인 엔티티로부터 DTO 생성하는 정적 팩토리 메서드
    public static StudyMaterialResponse from(StudyMaterial studyMaterial, String uploaderNickname, Integer uploaderTrustScore) {
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
                studyMaterial.getUploaderId().value(),
                uploaderTrustScore,
                studyMaterial.getCreatedAt(),
                studyMaterial.getDisplayTitle()
        );
    }
    public static StudyMaterialResponse fromWithAnswers(StudyMaterial studyMaterial, String uploaderNickname, int trustScore) {
        // 기존 from 메서드와 동일 (정답 포함)
        return from(studyMaterial, uploaderNickname, trustScore);
    }

    public static StudyMaterialResponse fromPreview(StudyMaterial studyMaterial, String uploaderNickname, int trustScore) {
        List<QuestionResponse> previewQuestions = studyMaterial.getAllQuestions().stream()
                .map(question -> QuestionResponse.fromPreview(question))  // 정답 제외 버전
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
                previewQuestions,  // 👈 정답 없는 버전
                uploaderNickname,
                studyMaterial.getCreatedAt(),
                studyMaterial.getDisplayTitle()
        );
    }
}