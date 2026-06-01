package com.turfbook.backend.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class UserPrincipal extends User {

    private final Long id;

    public UserPrincipal(Long id,
                         String email,
                         String passwordHash,
                         boolean blocked,
                         Collection<? extends GrantedAuthority> authorities) {
        super(email, passwordHash, !blocked, true, true, !blocked, authorities);
        this.id = id;
    }
}
