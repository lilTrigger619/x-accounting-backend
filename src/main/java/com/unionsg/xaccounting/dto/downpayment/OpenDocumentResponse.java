package com.unionsg.xaccounting.dto.downpayment;

import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An invoice or bill a downpayment can still be applied to. */
@Data
public class OpenDocumentResponse {
    private SettlementDocumentType documentType;
    private Long documentId;
    private String documentNumber;
    private String reference;
    private LocalDate documentDate;
    private LocalDate dueDate;
    private String currency;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal balance;
}
