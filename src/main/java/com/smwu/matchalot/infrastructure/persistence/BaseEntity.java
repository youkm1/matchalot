package com.smwu.matchalot.infrastructure.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEntity {
    @Id
    private Long id;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    //R2DBCsms @PrePersist가 없다 -> 수동 설정
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        this.updatedAt = LocalDateTime.now();
    }



    public void setTimestamps() {
        LocalDateTime now = LocalDateTime.now();

        if (this.id == null) {
            // 새로운 엔티티 - INSERT
            this.createdAt = now;
            this.updatedAt = now;
        } else {
            // 기존 엔티티 - UPDATE
            this.updatedAt = now;
        }
    }

    public void setUpdatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
        this.updatedAt = LocalDateTime.now();
    }
}
