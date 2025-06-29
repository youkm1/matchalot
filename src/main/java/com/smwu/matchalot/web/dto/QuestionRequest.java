package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.vo.Question;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionRequest(
        @NotNull(message = "문제 번호는 필수입니다")
        @Min(value = 1, message = "문제 번호는 1 이상이어야 합니다")
        Integer number,

        @NotBlank(message = "문제 내용은 필수입니다")
        @Size(max = 2000, message = "문제 내용은 2000자 이하여야 합니다")
        String content,

        @NotBlank(message = "답안은 필수입니다")
        @Size(max = 1000, message = "답안은 1000자 이하여야 합니다")
        String answer,

        @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
        String explanation
) {
        public Question toQuestion() {
                return Question.of(number, content, answer, explanation);
        }
}
