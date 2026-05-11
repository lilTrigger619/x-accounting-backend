package com.unionsg.xaccounting.repository.invoice;

import com.unionsg.xaccounting.entity.invoice.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceRepositoryCustom {

    Page<Invoice> searchInvoices(
            String search,
            Long customerId,
            String status,
            Pageable pageable
    );

}
