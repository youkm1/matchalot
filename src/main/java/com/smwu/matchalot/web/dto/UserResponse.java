package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long Id,
        String nickname,
        String email,
        int trustScore,//신뢰도 점수 -5 ~ 5
        LocalDateTime createdAt,
        String role,
        String roleDescription,
        boolean canUpload,
        boolean canMatch,
        boolean isAdmin
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getEmail().value(),
                user.getNickname(),
                user.getTrustScore().value(),
                user.getCreatedAt(),
                user.getRole().name(),
                user.getRole().getDescription(),
                user.canUploadMaterial(),
                user.canRequestMatch(),
                user.isAdmin()
        );
    }
}
