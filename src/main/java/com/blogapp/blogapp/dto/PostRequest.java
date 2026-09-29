package com.blogapp.blogapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// categoryId is optional
public record PostRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        Long categoryId
) {
}
