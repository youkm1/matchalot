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

    // StudyMaterialMapper.java의 toDomain 메서드에서 status 매핑 확인
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
                    new Semester(entity.getYear(), entity.getSeason()),
                    questions,
                    MaterialStatus.valueOf(entity.getStatus()), // status 매핑 추가
                    entity.getCreatedAt()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Questions JSON 파싱 실패", e);
        }
    }

    // toEntity 메서드에서도 status 매핑 확인
    public StudyMaterialEntity toEntity(StudyMaterial domain) {
        try {
            StudyMaterialEntity entity = new StudyMaterialEntity();

            if (domain.getId() != null) {
                entity.setId(domain.getId().value());
            }

            entity.setUploaderId(domain.getUploaderId().value());
            entity.setTitle(domain.getTitle());
            entity.setSubject(domain.getSubject().name());
            entity.setExamType(domain.getExamType().type());
            entity.setYear(domain.getSemester().year());
            entity.setSeason(domain.getSemester().season());
            entity.setStatus(domain.getStatus().name()); // status 매핑 추가

            // Questions 객체를 JSON으로 변환
            List<QuestionDto> questionDtos = domain.getAllQuestions().stream()
                    .map(q -> new QuestionDto(q.getNumber(), q.getContent(), q.getAnswer(), q.getDescription()))
                    .toList();

            String questionsJson = objectMapper.writeValueAsString(questionDtos);
            entity.setQuestionsJson(questionsJson);
            entity.setCreatedAt(domain.getCreatedAt());

            return entity;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Questions JSON 변환 실패", e);
        }
    }