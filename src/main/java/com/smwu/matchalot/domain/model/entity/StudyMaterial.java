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
    private MaterialStatus status;
    private  LocalDateTime createdAt;

    public StudyMaterial(UserId uploaderId, String title, Subject subject, ExamType examType, Semester semester, Questions questions) {
        this(null, uploaderId, title, subject, examType, semester, questions,MaterialStatus.PENDING, LocalDateTime.now());
    }

    public StudyMaterial(UserId uploaderId, String title, Subject subject, ExamType examType, Semester semester, Questions questions, MaterialStatus status) {
        this(null, uploaderId, title, subject, examType, semester, questions, status, LocalDateTime.now());
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

    public StudyMaterial approve() {
        if (status != MaterialStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태의 자료만 승인 가능합니다.");
        }
        return new StudyMaterial(id, uploaderId, title, subject, examType, semester, questions, MaterialStatus.APPROVED, createdAt);
    }

    public StudyMaterial reject() {
        if (status != MaterialStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태의 자료만 거절 가능합니다.");
        }
        return new StudyMaterial(id, uploaderId, title, subject, examType, semester, questions, MaterialStatus.REJECTED, createdAt);
    }

    public boolean canBeUsedForMatching() {
        return status.canBeUsedForMatching();
    }

    public boolean isPendingApproval() {
        return status.isPending();
    }

    public boolean isApproved() {
        return status.isApproved();
    }

    public boolean isRejected() {
        return status.isRejected();
    }
}
