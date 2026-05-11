package com.unionsg.xaccounting.dto.invoice;

import com.unionsg.xaccounting.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class InvoiceSummaryResponse {

    private Long id;

    private String invoiceNumber;

    private String customerName;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private InvoiceStatus status;

    private BigDecimal totalAmount;

    private BigDecimal totalDue;

}
