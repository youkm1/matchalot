package com.smwu.matchalot.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.infrastructure.persistence.StudyMaterialEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StudyMaterialMapper {

    private final ObjectMapper objectMapper;

    public StudyMaterial toDomain(StudyMaterialEntity entity) {
        try {
            // JSON을 Questions 객체로 변환
            QuestionDto[] questionDtos = objectMapper.readValue(entity.getQuestionsJson(), QuestionDto[].class);
            List<Question> questionList = java.util.Arrays.stream(questionDtos)
                    .map(dto -> Question.of(dto.number(), dto.content(), dto.answer(), dto.description()))
                    .toList();
            Questions questions = new Questions(questionList);

            return new StudyMaterial(
                    entity.getId() != null ? StudyMaterialId.of(entity.getId()) : null,
                    UserId.of(entity.getUploaderId()),
                    entity.getTitle(),
                    Subject.of(entity.getSubject()),
                    ExamType.of(entity.getExamType()),
                    Semester.of(entity.getYear(), entity.getSeason()),
                    questions,
                    entity.getCreatedAt()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("문제 데이터 파싱 오류", e);
        }
    }
    public StudyMaterialEntity toEntity(StudyMaterial domain) {
        try {
            StudyMaterialEntity entity = new StudyMaterialEntity();

            if (domain.getId() != null) {
                entity.setId(domain.getId().value());
            }
            entity.setUploaderId(domain.getUploaderId().value());
            entity.setSubject(domain.getSubject().name());
            entity.setExamType(domain.getExamType().type());
            entity.setYear(domain.getSemester().year());
            entity.setSeason(domain.getSemester().season());
            entity.setTitle(domain.getTitle());
            entity.setQuestionCount(domain.getQuestionCount()); // 캐시

            // Questions를 JSON으로 변환
            List<QuestionDto> questionDtos = domain.getAllQuestions().stream()
                    .map(q -> new QuestionDto(q.number(), q.content(), q.answer(), q.explanation()))
                    .toList();
            entity.setQuestionsJson(objectMapper.writeValueAsString(questionDtos));

            entity.setCreatedAt(domain.getCreatedAt());

            return entity;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("문제 데이터 직렬화 오류", e);
        }
    }

    // JSON 직렬화용 DTO
    public record QuestionDto(
            int number,
            String content,
            String answer,
            String description
    ) {}
}
