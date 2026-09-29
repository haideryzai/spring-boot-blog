package com.blogapp.blogapp.service;

import com.blogapp.blogapp.dto.AuthResponse;
import com.blogapp.blogapp.dto.LoginRequest;
import com.blogapp.blogapp.dto.RegisterRequest;
import com.blogapp.blogapp.dto.UserResponse;
import com.blogapp.blogapp.entity.User;
import com.blogapp.blogapp.exception.DuplicateResourceException;
import com.blogapp.blogapp.repository.UserRepository;
import com.blogapp.blogapp.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = UserService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(email), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = UserService.normalizeEmail(request.email());
        // Throws BadCredentialsException (-> 401) if the email/password pair is wrong
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email).orElseThrow();
        return new AuthResponse(jwtService.generateToken(email), UserResponse.from(user));
    }
}
