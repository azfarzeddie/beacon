package com.beacon.model.response;

import com.beacon.model.entity.User;

public record CreateUserResponse(Long id, String externalId, String name, String email, String phone) {
    public static CreateUserResponse from(User user) {
        return new CreateUserResponse(user.getId(), user.getExternalId(), user.getName(), user.getEmail(),
                user.getPhone());
    }
}
