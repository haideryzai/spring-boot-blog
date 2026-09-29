package com.blogapp.blogapp.security;

import com.blogapp.blogapp.entity.Role;
import com.blogapp.blogapp.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// The logged-in user as Spring Security sees it. Controllers receive it via @AuthenticationPrincipal.
public class SecurityUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Role role;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
    }

    public Long getId() {
        return id;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    // True if this user owns the resource (by owner id) or is an admin
    public boolean canModify(Long ownerId) {
        return isAdmin() || id.equals(ownerId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
