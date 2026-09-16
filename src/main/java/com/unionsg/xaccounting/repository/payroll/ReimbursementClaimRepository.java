package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.ReimbursementClaim;
import com.unionsg.xaccounting.enums.ReimbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReimbursementClaimRepository extends JpaRepository<ReimbursementClaim, Long> {
    List<ReimbursementClaim> findByEmployeeId(Long employeeId);
    List<ReimbursementClaim> findByStatus(ReimbursementStatus status);
}
