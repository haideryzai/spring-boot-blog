package com.blogapp.blogapp.service;

import com.blogapp.blogapp.dto.CommentRequest;
import com.blogapp.blogapp.dto.CommentResponse;
import com.blogapp.blogapp.dto.PageResponse;
import com.blogapp.blogapp.entity.Comment;
import com.blogapp.blogapp.exception.ResourceNotFoundException;
import com.blogapp.blogapp.repository.CommentRepository;
import com.blogapp.blogapp.repository.PostRepository;
import com.blogapp.blogapp.repository.UserRepository;
import com.blogapp.blogapp.security.SecurityUser;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          PostService postService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postService = postService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", postId);
        }
        return PageResponse.from(commentRepository.findByPostId(postId, pageable), CommentResponse::from);
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request, SecurityUser currentUser) {
        Comment comment = new Comment();
        comment.setPost(postService.findPost(postId));
        comment.setAuthor(userRepository.getReferenceById(currentUser.getId()));
        comment.setContent(request.content().trim());
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse updateComment(Long id, CommentRequest request, SecurityUser currentUser) {
        Comment comment = findComment(id);
        if (!currentUser.canModify(comment.getAuthor().getId())) {
            throw new AccessDeniedException("You can only edit your own comments");
        }
        comment.setContent(request.content().trim());
        commentRepository.flush();
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long id, SecurityUser currentUser) {
        Comment comment = findComment(id);
        // The comment's author, the post's author, and admins may delete a comment
        boolean allowed = currentUser.canModify(comment.getAuthor().getId())
                || currentUser.getId().equals(comment.getPost().getAuthor().getId());
        if (!allowed) {
            throw new AccessDeniedException("You can't delete this comment");
        }
        commentRepository.delete(comment);
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }
}
