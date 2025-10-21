package com.smwu.matchalot.web.dto;

import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import jakarta.validation.constraints.NotNull;

public record MatchRequestDto(
        @NotNull(message = "매칭할 족보의 ID를 입력하세요")
        Long requesterMaterialId,

        @NotNull(message = "상대방 ID 는 필수입니다.")
        Long receiverId,

        // receiverMaterialId를 Optional로 변경 (URL에서 가져올 수 있도록)
        Long receiverMaterialId,

        String message
) {
    public StudyMaterialId getRequesterMaterialId() {
        return StudyMaterialId.of(requesterMaterialId);
    }
    public UserId getReceiverId() {
        return UserId.of(receiverId);
    }
    public StudyMaterialId getReceiverMaterialId() {
        return StudyMaterialId.of(receiverMaterialId);
    }
}
