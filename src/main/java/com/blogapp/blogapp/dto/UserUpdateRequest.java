package com.blogapp.blogapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// password is optional: leave it null to keep the current one
public record UserUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email String email,
        @Size(max = 500) String bio,
        @Size(min = 8, max = 100) String password
) {
}
