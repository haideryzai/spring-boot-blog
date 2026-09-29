package com.blogapp.blogapp.dto;

import com.blogapp.blogapp.entity.Post;

import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String content,
        AuthorSummary author,
        CategoryResponse category,
        Instant createdAt,
        Instant updatedAt
) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                AuthorSummary.from(post.getAuthor()),
                post.getCategory() == null ? null : CategoryResponse.from(post.getCategory()),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
