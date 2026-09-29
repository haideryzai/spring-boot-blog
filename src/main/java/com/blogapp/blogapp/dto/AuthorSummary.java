package com.blogapp.blogapp.dto;

import com.blogapp.blogapp.entity.User;

public record AuthorSummary(Long id, String name) {

    public static AuthorSummary from(User user) {
        return new AuthorSummary(user.getId(), user.getName());
    }
}
