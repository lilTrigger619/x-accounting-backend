package com.unionsg.xaccounting.repository.branch;

import com.unionsg.xaccounting.entity.branch.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {

    @Query("""
        SELECT cc FROM CostCenter cc JOIN FETCH cc.branch b
        WHERE cc.deleted = false
          AND (:branchId IS NULL OR b.id = :branchId)
          AND (:activeOnly = false OR (cc.active = true AND b.active = true))
          AND (:search IS NULL OR LOWER(cc.name) LIKE :search OR LOWER(cc.code) LIKE :search)
        ORDER BY b.name ASC, cc.branchDefault DESC, cc.name ASC
        """)
    List<CostCenter> search(@Param("branchId") Long branchId,
                            @Param("activeOnly") boolean activeOnly,
                            @Param("search") String search);

    List<CostCenter> findByBranchIdAndDeletedFalse(Long branchId);

    List<CostCenter> findByBranchIdInAndDeletedFalse(Collection<Long> branchIds);

    Optional<CostCenter> findFirstByBranchIdAndBranchDefaultTrueAndDeletedFalse(Long branchId);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(String code, Long id);

    boolean existsByBranchIdAndNameIgnoreCaseAndDeletedFalse(Long branchId, String name);

    boolean existsByBranchIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(Long branchId, String name, Long id);

    boolean existsByBranchIdAndDeletedFalseAndBranchDefaultFalse(Long branchId);
}
