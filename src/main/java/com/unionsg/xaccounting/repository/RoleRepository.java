// repository/RoleRepository.java
package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.User.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByName(String name);
}