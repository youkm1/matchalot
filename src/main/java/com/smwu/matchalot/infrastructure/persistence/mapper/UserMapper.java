package com.smwu.matchalot.infrastructure.persistence.mapper;

import com.smwu.matchalot.domain.model.entity.User;
import com.smwu.matchalot.domain.model.vo.Email;
import com.smwu.matchalot.domain.model.vo.TrustScore;
import com.smwu.matchalot.domain.model.vo.UserId;
import com.smwu.matchalot.domain.model.vo.UserRole;
import com.smwu.matchalot.infrastructure.persistence.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toDomain(UserEntity entity) {
        return new User(
                UserId.of(entity.getId()),           // Long → UserId VO
                Email.of(entity.getEmail()),         // String → Email VO
                entity.getNickname(),
                new TrustScore(entity.getTrustScore()),
                UserRole.valueOf(entity.getRole()),
                entity.getCreatedAt()
        );
    }

    public UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId().value());
        }        // UserId VO → Long
        entity.setEmail(domain.getEmail().value());  // Email VO → String
        entity.setNickname(domain.getNickname());
        entity.setTrustScore(domain.getTrustScore().value());
        entity.setRole(domain.getRole().name());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
