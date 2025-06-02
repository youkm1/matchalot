package com.smwu.matchalot.web.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long Id,
        String nickname,
        String email,
        int trustScore,//신뢰도 점수 -5 ~ 5
        LocalDateTime createdAt
) {
}
