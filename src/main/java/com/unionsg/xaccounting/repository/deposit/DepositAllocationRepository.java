package com.unionsg.xaccounting.repository.deposit;

import com.unionsg.xaccounting.entity.deposit.DepositAllocation;
import com.unionsg.xaccounting.enums.deposit.DepositAllocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DepositAllocationRepository extends JpaRepository<DepositAllocation, Long> {

    List<DepositAllocation> findByDepositIdOrderByAllocationDateAscIdAsc(Long depositId);

    @Query("""
            SELECT a FROM DepositAllocation a JOIN a.deposit d
            WHERE d.deleted = false
              AND (:type IS NULL OR a.allocationType = :type)
              AND (:fromDate IS NULL OR a.allocationDate >= :fromDate)
              AND (:toDate IS NULL OR a.allocationDate <= :toDate)
            ORDER BY a.allocationDate DESC, a.id DESC
            """)
    Page<DepositAllocation> search(
            @Param("type") DepositAllocationType type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
