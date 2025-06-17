package com.smwu.matchalot.web.dto;

public record LoginResponse(
        String token,
        UserResponse user,
        boolean isNewUser,
        String message
) {
    public static LoginResponse success(String token, UserResponse user, boolean isNewUser) {
        return new LoginResponse(token, user, isNewUser, "완료!");
    }
    public static LoginResponse fail(String message) {
        return new LoginResponse(null, null, false, message);
    }
}
