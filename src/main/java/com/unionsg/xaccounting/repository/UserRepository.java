package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    // for paginated list — optionally filter by status
    Page<User> findAllByStatus(UserStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.status = 'DISABLED' WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.status = 'ACTIVE' WHERE u.id = :id")
    void activateById(@Param("id") Long id);
}