package com.smwu.matchalot.domain.model.entity;

import com.smwu.matchalot.domain.model.vo.MatchId;
import com.smwu.matchalot.domain.model.vo.MatchStatus;
import com.smwu.matchalot.domain.model.vo.StudyMaterialId;
import com.smwu.matchalot.domain.model.vo.UserId;
import lombok.AllArgsConstructor;
import lombok.Getter;


import java.time.LocalDateTime;

import static com.smwu.matchalot.domain.model.vo.MatchStatus.ACCEPTED;

@Getter
@AllArgsConstructor
public class Match {
    private final MatchId id;
    private final UserId requesterId;
    private final UserId receiverId;
    private final StudyMaterialId requesterMaterialId;
    private final StudyMaterialId receiverMaterialId;
    private final MatchStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiredAt; //매칭만료는24시간

    public Match(UserId requesterId, UserId receiverId, StudyMaterialId requesterMaterialId, StudyMaterialId receiverMaterialId) {
        this(null, requesterId, receiverId, requesterMaterialId, receiverMaterialId, MatchStatus.PENDING, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    }

    public Match accept() {
        if (status != MatchStatus.PENDING) {
            throw new IllegalStateException("대기 중인 매칭만 수락할 수 있습니다");
        }
        if (LocalDateTime.now().isAfter(expiredAt)) {
            throw new IllegalStateException("매칭이 이미 만료되었습니다");
        }
        return new Match(id, requesterId, receiverId, requesterMaterialId, receiverMaterialId, ACCEPTED, createdAt, expiredAt);
    }

    public Match reject() {
        if (status != MatchStatus.PENDING) {
            throw new IllegalStateException("대기 중인 매칭만 거절할 수 있습니다");
        }
        return new Match(id, requesterId, receiverId, requesterMaterialId, receiverMaterialId, MatchStatus.REJECTED, createdAt, expiredAt);
    }

    public Match expire() {
        if (status.isFinished()) {
            throw new IllegalStateException("매칭이 완료된 상태입니다");
        }
        return new Match(id, requesterId, receiverId, requesterMaterialId, receiverMaterialId, MatchStatus.EXPIRED, createdAt, expiredAt);
    }

    public Match complete() {
        if (status != ACCEPTED) {
            throw new IllegalStateException("수락된 매칭만 완료할 수 있습니다");
        }
        return new Match(id, requesterId, receiverId, requesterMaterialId, receiverMaterialId, MatchStatus.COMPLETED, createdAt, expiredAt);
    }

    //이 매칭의 참여자인지 확인하기
    public boolean isParticipant(UserId userId) {
        return userId.equals(requesterId) || userId.equals(receiverId);
    }

    public boolean isRequester(UserId userId) {
        return requesterId.equals(userId);
    }

    public boolean isReceiver(UserId userId) {
        return receiverId.equals(userId);
    }

    public UserId getOtherParticipant(UserId userId) {
        if (requesterId.equals(userId)) {
            return receiverId;
        } else if (receiverId.equals(userId)) {
            return requesterId;
        } else {
            throw new IllegalArgumentException("해당 사용자는 이 매칭 참여자가 아닙니다");
        }
    }

    public StudyMaterialId getUserMaterial(UserId userId) {
        if (requesterId.equals(userId)) {
            return requesterMaterialId;
        } else if (receiverId.equals(userId)) {
            return receiverMaterialId;
        } else {
            throw new IllegalArgumentException("해당 사용자는 매칭 참여자가 아니므로 족보를 찾을 수 없습니다");
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }
    public StudyMaterialId getOtherUserMaterial(UserId userId) {
        if (requesterId.equals(userId)) {
            return receiverMaterialId;
        } else if (receiverId.equals(userId)) {
            return requesterMaterialId;
        } else {
            throw new IllegalArgumentException("해당 사용자는 매칭 참여자가 아닌이므로 족보를 찾을 수 없습니다");
        }
    }
}
