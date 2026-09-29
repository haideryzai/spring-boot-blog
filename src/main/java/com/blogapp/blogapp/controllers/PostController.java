package com.blogapp.blogapp.controllers;

import com.blogapp.blogapp.dto.PageResponse;
import com.blogapp.blogapp.dto.PostRequest;
import com.blogapp.blogapp.dto.PostResponse;
import com.blogapp.blogapp.security.SecurityUser;
import com.blogapp.blogapp.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // e.g. GET /api/posts?search=spring&categoryId=1&page=0&size=10&sort=createdAt,desc
    @GetMapping
    public PageResponse<PostResponse> getPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.getPosts(categoryId, authorId, search, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest request,
                                                   @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id,
                                                   @Valid @RequestBody PostRequest request,
                                                   @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(postService.updatePost(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, @AuthenticationPrincipal SecurityUser currentUser) {
        postService.deletePost(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
