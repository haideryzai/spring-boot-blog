package com.blogapp.blogapp.dto;

import com.blogapp.blogapp.entity.Comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String content,
        Long postId,
        AuthorSummary author,
        Instant createdAt,
        Instant updatedAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getPost().getId(),
                AuthorSummary.from(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
