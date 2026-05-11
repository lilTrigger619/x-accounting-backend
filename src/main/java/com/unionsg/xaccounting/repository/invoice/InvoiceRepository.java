package com.unionsg.xaccounting.repository.invoice;

import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
