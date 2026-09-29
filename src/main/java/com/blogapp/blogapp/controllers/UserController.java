package com.blogapp.blogapp.controllers;

import com.blogapp.blogapp.dto.PageResponse;
import com.blogapp.blogapp.dto.UserResponse;
import com.blogapp.blogapp.dto.UserUpdateRequest;
import com.blogapp.blogapp.security.SecurityUser;
import com.blogapp.blogapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Admin only (see SecurityConfig)
    @GetMapping
    public PageResponse<UserResponse> getAllUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(userService.getUserById(currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserUpdateRequest request,
                                                   @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @AuthenticationPrincipal SecurityUser currentUser) {
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
