package com.smwu.matchalot.infrastructure.persistence;

//DB 매핑 객체

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
@Getter @Setter
public class UserEntity extends BaseEntity {

    @Column("email")
    private String email;

    @Column("nickname")
    private String nickname;

    @Column("trust_score")
    private int trustScore;

    @Column("role")
    private String role;

}
