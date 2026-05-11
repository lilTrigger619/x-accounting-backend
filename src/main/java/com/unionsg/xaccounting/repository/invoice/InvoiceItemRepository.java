package com.unionsg.xaccounting.repository.invoice;

import com.unionsg.xaccounting.entity.invoice.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
}
