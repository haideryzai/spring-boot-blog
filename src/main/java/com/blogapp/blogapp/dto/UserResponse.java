package com.blogapp.blogapp.dto;

import com.blogapp.blogapp.entity.Role;
import com.blogapp.blogapp.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, String bio, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getBio(),
                user.getRole(), user.getCreatedAt());
    }
}
