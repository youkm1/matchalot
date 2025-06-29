package com.smwu.matchalot.infrastructure.persistence.mapper;

import com.smwu.matchalot.domain.model.entity.Report;
import com.smwu.matchalot.domain.model.vo.*;
import com.smwu.matchalot.infrastructure.persistence.ReportEntity;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {
    public Report toDomain(ReportEntity entity) {
        return new Report(
                entity.getId() != null ? ReportId.of(entity.getId()) : null,
                UserId.of(entity.getReporterId()),
                UserId.of(entity.getReportedUserId()),
                entity.getMaterialId() != null ? StudyMaterialId.of(entity.getMaterialId()) : null,
                ReportType.valueOf(entity.getType()),
                entity.getDescription(),
                ReportStatus.valueOf(entity.getStatus()),
                entity.getAdminNote(),
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }

    public ReportEntity toEntity(Report domain) {
        ReportEntity entity = new ReportEntity();

        if (domain.getId() != null) {
            entity.setId(domain.getId().value());
        }

        entity.setReporterId(domain.getReporterId().value());
        entity.setReportedUserId(domain.getReportedUserId().value());
        entity.setMaterialId(domain.getMaterialId() != null ? domain.getMaterialId().value() : null);
        entity.setType(domain.getType().name());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus().name());
        entity.setAdminNote(domain.getAdminNote());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setResolvedAt(domain.getResolvedAt());

        return entity;
    }
}
