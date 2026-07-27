package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.Person;
import com.tuan.chatserver.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByPerson(Person person);
}
