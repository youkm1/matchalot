package com.smwu.matchalot.domain.model.vo;


import java.util.List;
import java.util.stream.Collectors;

public record Questions(List<Question> questions) {
    public Questions {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("문제는 필수입니다.");
        }
        var questionNumbers = questions.stream()
                .map(Question::number)
                .collect(Collectors.toSet());
        if (questionNumbers.size() != questions.size()) {
            throw new IllegalArgumentException("문제 번호 중복");
        }
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public Question getQuestion(int number) {
        return questions.stream()
                .filter(q -> q.number() == number)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("문제 %d을 찾을 수 없습니다.", number)));

    }

    public List<Question> getSortedQuestions() {
        return questions.stream()
                .sorted((q1,q2) -> Integer.compare(q1.number(), q2.number())) //record는 자동 private final 필드임. getter 가 자동배치되므로 number() 사용
                .toList();
    }
}
