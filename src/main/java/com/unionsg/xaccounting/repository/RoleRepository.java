// repository/RoleRepository.java
package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.User.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    boolean existsByName(String name);
}