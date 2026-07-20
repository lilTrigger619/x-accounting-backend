package com.unionsg.xaccounting.repository.invoice;

import com.unionsg.xaccounting.dto.invoice.InvoiceTotalsResponse;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.InvoiceStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {


    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByIssueDateBetween(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    Page<Invoice> findByCustomerId(
            Long customerId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.unionsg.xaccounting.dto.invoice.InvoiceTotalsResponse(
                COALESCE(SUM(CASE WHEN i.status = :paidStatus THEN i.totalAmount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN i.status = :overdueStatus THEN i.totalAmount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN i.status IN (:pendingStatuses) THEN i.totalAmount ELSE 0 END), 0),
                COALESCE(SUM(i.totalAmount), 0)
            )
            FROM Invoice i
            """)
    InvoiceTotalsResponse getInvoiceTotals(
            @Param("paidStatus") InvoiceStatus paidStatus,
            @Param("overdueStatus") InvoiceStatus overdueStatus,
            @Param("pendingStatuses") java.util.List<InvoiceStatus> pendingStatuses
    );


}
