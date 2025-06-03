package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record StudyMaterialUploadRequest(
        @NotBlank(message = "과목명은 필수입니다")
        String subject,

        @NotBlank(message = "시험 유형은 필수입니다")
        String examType,

        @NotNull(message = "연도는 필수입니다")
        @Min(value = 2018, message = "연도는 2018년 이상이어야 합니다")
        @Max(value = 2025, message = "연도는 2025년 이하여야 합니다")
        Integer year,

        @NotBlank(message = "학기는 필수입니다")
        String season,

        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 5, max = 200, message = "제목은 5자 이상 200자 이하여야 합니다")
        String title,

        @Valid
        @NotEmpty(message = "문제는 최소 1개 이상이어야 합니다")
        @Size(max = 50, message = "문제는 최대 50개까지 가능합니다")
        List<QuestionRequest> questions
) {
    public Subject getSubjectVO() {
        return Subject.of(subject);
    }

    public ExamType getExamTypeVO() {
        return ExamType.of(examType);
    }

    public Semester getSemesterVO() {
        return Semester.of(year, season);
    }

    public Questions getQuestionsVO() {
        List<Question> questionList = questions.stream()
                .map(QuestionRequest::toQuestion)
                .toList();
        return new Questions(questionList);
    }
}
