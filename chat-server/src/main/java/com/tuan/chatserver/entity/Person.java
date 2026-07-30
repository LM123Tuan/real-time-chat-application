package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "person")
public abstract class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Pattern(
            regexp = "^[a-zA-Z0-9_]{5,20}$",
            message = "Username must contain only letters, numbers, and underscores, and be between 5 and 20 characters long"
    )
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = true)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    @Column(nullable = false)
    private boolean isActive;
    @Column(nullable = false)
    private int tokenVersion = 0;

    public Person(){}
    public Person(String username, String password, UserRole role, boolean isActive) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
    }

    public Long getId() {
        return this.id;
    }
    public String getUsername() {
        return this.username;
    }
    public String getPassword() {
        return this.password;
    }
    public UserRole getRole() {
        return this.role;
    }
    public boolean isActive() {
        return this.isActive;
    }
    public int getTokenVersion(){
        return this.tokenVersion;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setRole(UserRole role) {
        this.role = role;
    }
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    public void setTokenVersion(int tokenVersion){
        this.tokenVersion=tokenVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(id, person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}