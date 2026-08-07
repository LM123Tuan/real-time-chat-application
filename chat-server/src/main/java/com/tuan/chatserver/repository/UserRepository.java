package com.tuan.chatserver.repository;

import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.AuthProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameAndIsActiveTrue(String username);
    Optional<User> findByEmailAndIsActiveTrue(String email);
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
    //boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);

    @Query("SELECT u FROM User u WHERE u.username LIKE CONCAT('%', :keyword, '%') ORDER BY u.id DESC")
    List<User> findByUsernameContainingOfFirstPage(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.username LIKE CONCAT('%', :keyword, '%') AND u.id < :cursorId ORDER BY u.id DESC")
    List<User> findByUsernameContainingOfNextPage(@Param("keyword") String keyword,
                                                  @Param("cursorId") Long cursorId,
                                                  Pageable pageable);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByIsActive(Boolean isActive);
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    Set<User> findByIdIn(@Param("ids") Set<Long> ids);

    @Query("SELECT u FROM User u ORDER BY u.id DESC")
    List<User> findAllOfFirstPage(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id < :cursorId ORDER BY u.id DESC")
    List<User> findAllOfNextPage(@Param("cursorId") Long cursorId, Pageable pageable);
}
