package com.unionsg.xaccounting.repository;
//import com.unionsg.xaccounting.entity.UserEntity;
import com.unionsg.xaccounting.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.unionsg.xaccounting.enums.UserStatus;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByEmailAndStatus(String  email, UserStatus status);
}
