package com.smwu.matchalot.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequestDto(
        @NotNull(message = "신고 대상 사용자 ID는 필수입니다")
        Long reportedUserId,

        Long materialId,  // 선택적: 자료 신고인 경우

        @NotBlank(message = "신고 유형은 필수입니다")
        String type,      // ReportType enum 값

        @NotBlank(message = "신고 사유는 필수입니다")
        @Size(min = 10, max = 1000, message = "신고 사유는 10자 이상 1000자 이하여야 합니다")
        String description
) {}