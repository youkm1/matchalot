package com.smwu.matchalot.domain.model.entity;

import com.smwu.matchalot.domain.model.vo.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
public class StudyMaterial {
    private final StudyMaterialId id;
    private final UserId uploaderId;
    private final String title;
    private final Subject subject;
    private final ExamType examType;
    private final Semester semester;
    private final Questions questions;
    private final LocalDateTime createdAt;

    public StudyMaterial(UserId uploaderId, String title, Subject subject, ExamType examType, Semester semester, Questions questions) {
        this(null, uploaderId, title, subject, examType, semester, questions, LocalDateTime.now());
    }

    public boolean isUploadedBy(UserId userId) {
        return uploaderId.equals(userId);
    }

    public int getQuestionCount() {
        return questions.getQuestionCount();
    }

    public String getDisplayTitle() {
        return String.format("%s (%s) %s (%s)",
                subject.name(),
                semester.getDisplayName(),
                examType.type(),
                getQuestionCount());
    }


    public Question getQuestion(int number) {
        return questions.getQuestion(number);
    }


    public List<Question> getAllQuestions() {
        return questions.getSortedQuestions();
    }

}
