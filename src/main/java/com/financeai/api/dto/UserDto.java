package com.financeai.api.dto;

import com.financeai.domain.entity.User;
import java.time.LocalDateTime;

public record UserDto(
    String id,
    String email,
    String name,
    String systemRole,
    LocalDateTime createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
            user.getId() != null ? user.getId().toString() : null,
            user.getEmail(),
            user.getName(),
            user.getSystemRole(),
            user.getCreatedAt()
        );
    }
}
