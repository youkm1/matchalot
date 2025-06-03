package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.vo.Question;

public record QuestionResponse(int number,
                               String content,
                               String answer,
                               String description
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.number(),
                question.content(),
                question.answer(),
                question.explanation()
        );
    }
}
