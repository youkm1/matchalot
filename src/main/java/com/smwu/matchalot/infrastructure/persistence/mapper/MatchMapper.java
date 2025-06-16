package com.smwu.matchalot.infrastructure.persistence.mapper;

import com.smwu.matchalot.domain.model.entity.Match;
import com.smwu.matchalot.domain.model.vo.MatchId;
import com.smwu.matchalot.domain.model.vo.MatchStatus;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.infrastructure.persistence.MatchEntity;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {
    public Match toDomain(MatchEntity entity) {
        return new Match(
                entity.getId() != null ? MatchId.of(entity.getId()) : null,
                UserId.of(entity.getRequesterId()),
                UserId.of(entity.getReceiverId()),
                StudyMaterialId.of(entity.getRequestedMaterialId()),
                StudyMaterialId.of(entity.getReceiverMaterialId()),
                MatchStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getExpiredAt()
        );
    }

    public MatchEntity toEntity(Match domain) {
        MatchEntity entity = new MatchEntity();

        if (domain.getId() != null) {
            entity.setId(domain.getId().value());
        }
        entity.setRequesterId(domain.getRequesterId().value());
        entity.setReceiverId(domain.getReceiverId().value());
        entity.setRequestedMaterialId(domain.getRequesterMaterialId().value());
        entity.setReceiverMaterialId(domain.getReceiverMaterialId().value());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setExpiredAt(domain.getExpiredAt());
        return entity;
    }
}
