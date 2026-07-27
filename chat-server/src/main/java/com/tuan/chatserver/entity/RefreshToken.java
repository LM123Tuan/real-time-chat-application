package com.tuan.chatserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    public RefreshToken(String token, LocalDateTime expiryDate, Person person){
        this.token = token;
        this.expiryDate = expiryDate;
        this.person = person;
    }

    public void setToken(String token){
        this.token=token;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void setExpiryDate(LocalDateTime expiryDate){
        this.expiryDate = expiryDate;
    }

    public void setPerson(Person person){
        this.person = person;
    }
}
