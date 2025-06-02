package com.smwu.matchalot.web.dto;

public record LoginResponse(
        String token,
        UserResponse user,
        boolean isNewUser
) {
}
