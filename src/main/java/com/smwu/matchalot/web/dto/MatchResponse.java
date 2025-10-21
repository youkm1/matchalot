package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.Match;

import java.time.LocalDateTime;

public record MatchResponse (
        Long matchId,
        Long requesterId,
        Long partnerId,
        String requesterNickname,
        String partnerNickname,
        Long requesterMaterialId,
        Long partnerMaterialId,
        String requesterMaterialTitle,
        String partnerMaterialTitle,
        String status,
        String statusDescription,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean isExpired
) {
    public static MatchResponse from(Match match,
                                   String requesterNickname,
                                   String receiverNickname,
                                   String requesterMaterialTitle,
                                   String receiverMaterialTitle,
                                   Long partnerMaterialId) {

        return new MatchResponse(
                match.getId() != null ? match.getId().value() : null,
                match.getRequesterId().value(),
                match.getReceiverId().value(),
                requesterNickname,
                receiverNickname,
                match.getRequesterMaterialId().value(),
                partnerMaterialId,
                requesterMaterialTitle,
                receiverMaterialTitle,
                match.getStatus().name(),
                match.getStatus().getDescription(),
                match.getCreatedAt(),
                match.getExpiredAt(),
                match.isExpired()
        );

    }
}

