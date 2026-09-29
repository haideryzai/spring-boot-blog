package com.blogapp.blogapp.service;

import com.blogapp.blogapp.dto.PageResponse;
import com.blogapp.blogapp.dto.UserResponse;
import com.blogapp.blogapp.dto.UserUpdateRequest;
import com.blogapp.blogapp.entity.User;
import com.blogapp.blogapp.exception.DuplicateResourceException;
import com.blogapp.blogapp.exception.ResourceNotFoundException;
import com.blogapp.blogapp.repository.UserRepository;
import com.blogapp.blogapp.security.SecurityUser;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, SecurityUser currentUser) {
        if (!currentUser.canModify(id)) {
            throw new AccessDeniedException("You can only update your own account");
        }
        User user = findUser(id);
        String email = normalizeEmail(request.email());
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setBio(request.bio());
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id, SecurityUser currentUser) {
        if (!currentUser.canModify(id)) {
            throw new AccessDeniedException("You can only delete your own account");
        }
        // Posts and comments are removed through the cascade on User
        userRepository.delete(findUser(id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
