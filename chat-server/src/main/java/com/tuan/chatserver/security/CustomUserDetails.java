package com.tuan.chatserver.security;

import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.Person;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Person person;

    public CustomUserDetails(Person person) {
        this.person = person;
    }

    @Override
    public String getUsername() {
        return person.getUsername();
    }

    @Override
    public String getPassword() {
        return person.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = (person instanceof Admin) ? "ROLE_ADMIN" : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return person.isActive();
    }

    public Person getPerson() {
        return person;
    }
}