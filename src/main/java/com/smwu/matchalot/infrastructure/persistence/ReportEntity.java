package com.smwu.matchalot.infrastructure.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("reports")
@Getter @Setter
public class ReportEntity extends BaseEntity {
    @Column("reporter_id")
    private Long reporterId;

    @Column("reported_user_id")
    private Long reportedUserId;

    @Column("material_id")
    private Long materialId;  // nullable

    @Column("type")
    private String type;

    @Column("description")
    private String description;

    @Column("status")
    private String status;

    @Column("admin_note")
    private String adminNote;

    @Column("resolved_at")
    private LocalDateTime resolvedAt;
}
