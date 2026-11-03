package com.unionsg.xaccounting.repository.branch;

import com.unionsg.xaccounting.entity.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByDeletedFalseOrderByNameAsc();
    List<Branch> findByActiveTrueAndDeletedFalseOrderByNameAsc();
    Optional<Branch> findFirstByHeadOfficeTrueAndDeletedFalse();
    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);
    boolean existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(String code, Long id);
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);
}
