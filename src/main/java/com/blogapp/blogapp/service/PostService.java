package com.blogapp.blogapp.service;

import com.blogapp.blogapp.dto.PageResponse;
import com.blogapp.blogapp.dto.PostRequest;
import com.blogapp.blogapp.dto.PostResponse;
import com.blogapp.blogapp.entity.Post;
import com.blogapp.blogapp.exception.ResourceNotFoundException;
import com.blogapp.blogapp.repository.PostRepository;
import com.blogapp.blogapp.repository.UserRepository;
import com.blogapp.blogapp.security.SecurityUser;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public PostService(PostRepository postRepository, UserRepository userRepository, CategoryService categoryService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPosts(Long categoryId, Long authorId, String search, Pageable pageable) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.from(postRepository.search(categoryId, authorId, term, pageable), PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        return PostResponse.from(findPost(id));
    }

    @Transactional
    public PostResponse createPost(PostRequest request, SecurityUser currentUser) {
        Post post = new Post();
        post.setAuthor(userRepository.getReferenceById(currentUser.getId()));
        applyRequest(post, request);
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, SecurityUser currentUser) {
        Post post = findPost(id);
        if (!currentUser.canModify(post.getAuthor().getId())) {
            throw new AccessDeniedException("You can only edit your own posts");
        }
        applyRequest(post, request);
        // Flush so @PreUpdate sets updatedAt before we build the response
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long id, SecurityUser currentUser) {
        Post post = findPost(id);
        if (!currentUser.canModify(post.getAuthor().getId())) {
            throw new AccessDeniedException("You can only delete your own posts");
        }
        postRepository.delete(post);
    }

    Post findPost(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    private void applyRequest(Post post, PostRequest request) {
        post.setTitle(request.title().trim());
        post.setContent(request.content());
        post.setCategory(request.categoryId() == null ? null : categoryService.findCategory(request.categoryId()));
    }
}
