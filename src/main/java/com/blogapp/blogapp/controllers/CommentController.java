package com.blogapp.blogapp.controllers;

import com.blogapp.blogapp.dto.CommentRequest;
import com.blogapp.blogapp.dto.CommentResponse;
import com.blogapp.blogapp.dto.PageResponse;
import com.blogapp.blogapp.security.SecurityUser;
import com.blogapp.blogapp.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommentResponse> getComments(@PathVariable Long postId,
                                                     @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return commentService.getComments(postId, pageable);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long postId,
                                                      @Valid @RequestBody CommentRequest request,
                                                      @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(postId, request, currentUser));
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id,
                                                         @Valid @RequestBody CommentRequest request,
                                                         @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(commentService.updateComment(id, request, currentUser));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                              @AuthenticationPrincipal SecurityUser currentUser) {
        commentService.deleteComment(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
