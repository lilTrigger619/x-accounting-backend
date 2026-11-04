package com.unionsg.xaccounting.repository.branch;

import com.unionsg.xaccounting.entity.branch.UserAccessScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserAccessScopeRepository extends JpaRepository<UserAccessScope, Long> {

    @Query("""
        SELECT DISTINCT s FROM UserAccessScope s
            LEFT JOIN FETCH s.branch
            LEFT JOIN FETCH s.costCenters
        WHERE s.user.id = :userId AND s.deleted = false
        """)
    List<UserAccessScope> findForUser(@Param("userId") UUID userId);

    boolean existsByUserIdAndDeletedFalse(UUID userId);

    boolean existsByBranchIdAndDeletedFalse(Long branchId);

    @Query("SELECT COUNT(s) > 0 FROM UserAccessScope s JOIN s.costCenters cc WHERE cc.id = :costCenterId AND s.deleted = false")
    boolean existsByCostCenter(@Param("costCenterId") Long costCenterId);
}
