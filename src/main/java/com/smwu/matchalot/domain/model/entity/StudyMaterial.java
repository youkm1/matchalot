package com.smwu.matchalot.domain.model.entity;

import com.smwu.matchalot.domain.model.vo.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
public class StudyMaterial {
    private StudyMaterialId id;
    private  UserId uploaderId;
    private  String title;
    private  Subject subject;
    private  ExamType examType;
    private  Semester semester;
    private  Questions questions;
    private  LocalDateTime createdAt;

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


    public StudyMaterialId getId() {
        return this.id;
    }

    // id 설정 메서드 (저장 후 사용)
    public void setId(StudyMaterialId id) {
        this.id = id;
    }

    public Question getQuestion(int number) {
        return questions.getQuestion(number);
    }


    public List<Question> getAllQuestions() {
        return questions.getSortedQuestions();
    }

}
