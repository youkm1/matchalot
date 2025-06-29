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
    public static QuestionResponse fromPreview(Question question) {
        return new QuestionResponse(
                question.number(),
                question.content(),
                "매칭 완료 후 확인 가능",  // 👈 정답 숨김
                "매칭 완료 후 확인 가능"   // 👈 해설 숨김
        );
    }
}
