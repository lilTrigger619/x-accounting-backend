package com.unionsg.xaccounting.repository.bill;

import com.unionsg.xaccounting.dto.bill.BillTotalsRow;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.enums.BillStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNumber(String billNumber);

    Page<Bill> findByStatus(BillStatus status, Pageable pageable);

    Page<Bill> findBySupplierId(Long supplierId, Pageable pageable);

    List<Bill> findBySupplierId(Long supplierId);

    @Query("""
            SELECT new com.unionsg.xaccounting.dto.bill.BillTotalsRow(
                COALESCE(SUM(CASE WHEN b.status = :paidStatus THEN b.totalAmount ELSE 0 END), 0),
                COALESCE(COUNT(CASE WHEN b.status = :paidStatus THEN 1 END), 0),
                COALESCE(SUM(CASE WHEN b.status = :overdueStatus THEN b.totalAmount ELSE 0 END), 0),
                COALESCE(COUNT(CASE WHEN b.status = :overdueStatus THEN 1 END), 0),
                COALESCE(SUM(CASE WHEN b.status IN (:pendingStatuses) THEN b.totalAmount ELSE 0 END), 0),
                COALESCE(COUNT(CASE WHEN b.status IN (:pendingStatuses) THEN 1 END), 0),
                COALESCE(SUM(b.totalAmount), 0),
                COALESCE(COUNT(b), 0)
            )
            FROM Bill b
            """)
    BillTotalsRow getBillTotals(
            @Param("paidStatus") BillStatus paidStatus,
            @Param("overdueStatus") BillStatus overdueStatus,
            @Param("pendingStatuses") List<BillStatus> pendingStatuses
    );

}
