package com.smwu.matchalot.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smwu.matchalot.domain.model.entity.StudyMaterial;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.infrastructure.persistence.StudyMaterialEntity;
import io.r2dbc.postgresql.codec.Json;
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
            String questionsJsonString = entity.getQuestionsJson().asString();
            QuestionDto[] questionDtos = objectMapper.readValue(questionsJsonString, QuestionDto[].class);
            List<Question> questionList = java.util.Arrays.stream(questionDtos)
                    .map(dto -> Question.of(dto.number(), dto.content(), dto.answer(), dto.explanation()))
                    .toList();
            Questions questions = new Questions(questionList);

            return new StudyMaterial(
                    entity.getId() != null ? StudyMaterialId.of(entity.getId()) : null,
                    UserId.of(entity.getUploaderId()),
                    entity.getTitle(),
                    Subject.of(entity.getSubject()),
                    ExamType.of(entity.getExamType()),
                    new Semester(entity.getYear(), entity.getSeason()),
                    questions,
                    entity.getStatus() != null ? MaterialStatus.valueOf(entity.getStatus()) : MaterialStatus.PENDING,
                    entity.getCreatedAt()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Questions JSON 파싱 실패", e);
        }
    }

    public StudyMaterialEntity toEntity(StudyMaterial domain) {
        try {
            StudyMaterialEntity entity = new StudyMaterialEntity();

            // id 처리: 실제 값이 있는 것도 확인 후 설정
            if (domain.getId() != null && domain.getId().value() != null) {
                entity.setId(domain.getId().value());
            }

            entity.setUploaderId(domain.getUploaderId().value());
            entity.setTitle(domain.getTitle());
            entity.setSubject(domain.getSubject().name());
            entity.setExamType(domain.getExamType().type());
            entity.setYear(domain.getSemester().year());
            entity.setSeason(domain.getSemester().season());
            entity.setStatus(domain.getStatus().name()); // ✅ status 매핑 추가
            entity.setQuestionCount(domain.getQuestionCount());

            // Questions를 JSON으로 변환
            List<QuestionDto> questionDtos = domain.getAllQuestions().stream()
                    .map(q -> new QuestionDto(q.number(), q.content(), q.answer(), q.explanation()))
                    .toList();

            String questionsJsonString = objectMapper.writeValueAsString(questionDtos);
            entity.setQuestionsJson(Json.of(questionsJsonString));
            entity.setCreatedAt(domain.getCreatedAt());

            return entity;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Questions JSON 변환 실패", e);
        }
    }


    public record QuestionDto(
            int number,
            String content,
            String answer,
            String explanation
    ) {}
}