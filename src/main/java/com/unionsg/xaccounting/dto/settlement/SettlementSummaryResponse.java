package com.unionsg.xaccounting.dto.settlement;

import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How an invoice or bill total has been settled: cash payments, each non-cash source
 * (keyed by {@code SettlementSourceType}), and what is still due.
 */
@Getter
@Setter
public class SettlementSummaryResponse {
    private SettlementDocumentType documentType;
    private Long documentId;
    private String documentNumber;
    private String currency;
    private BigDecimal originalAmount;
    /** Settled by customer receipts / supplier payments. */
    private BigDecimal paymentsApplied;
    /** Non-cash settlements by source type, e.g. {"DEPOSIT": 3000.00, "DOWNPAYMENT": 0}. */
    private Map<String, BigDecimal> appliedBySource = new LinkedHashMap<>();
    private BigDecimal totalSettled;
    private BigDecimal amountDue;
    private List<DocumentSettlementResponse> settlements = new ArrayList<>();
}
