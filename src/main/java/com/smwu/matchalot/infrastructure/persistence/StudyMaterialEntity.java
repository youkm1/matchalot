package com.smwu.matchalot.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.r2dbc.postgresql.codec.Json;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("study_material")
@Getter @Setter
public class StudyMaterialEntity extends BaseEntity {
    @Column("uploader_id")
    private Long uploaderId;

    @Column("subject")
    private String subject;

    @Column("exam_type")
    private String examType;

    @Column("year")
    private Integer year;

    @Column("season")
    private String season;

    @Column("title")
    private String title;

    @Column("questions")
    private Json questionsJson;  // JSONB로 저장될 문제들 (String으로 매핑)

    @Column("question_count")
    private Integer questionCount;
}
