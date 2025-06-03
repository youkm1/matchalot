package com.smwu.matchalot.domain.model.vo;


public record Question(
        int number,
        String content,
        String answer,
        String explanation
) {
    public Question {
        if (number <= 0) {
            throw new IllegalArgumentException("문제를 입력해주세요");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("문제 내용을 입력해주세요");
        }
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("답안을 입력해주세요");
        }
        if (explanation == null || explanation.trim().isEmpty()) {
            throw new IllegalArgumentException("설명을 입력해주세요");
        }
    }
    public static Question of(int number, String content, String answer, String explanation) {
        return new Question(number, content, answer, explanation);
    }
}
