package com.smwu.matchalot.infrastructure.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("matches")
@Getter @Setter
public class MatchEntity extends BaseEntity{

    @Column("requester_id")
    private Long requesterId;

    @Column("receiver_id")
    private Long receiverId;

    @Column("requested_material_id")
    private Long requestedMaterialId;

    @Column("receiver_material_id")
    private Long receiverMaterialId;

    @Column("status")
    private String status;

    @Column("expired_at")
    private LocalDateTime expiredAt;
}
