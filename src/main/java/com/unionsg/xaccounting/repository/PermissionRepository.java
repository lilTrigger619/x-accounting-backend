// repository/PermissionRepository.java
package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.User.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    boolean existsByNameAndGuardName(String name, String guardName);
}