package com.unionsg.xaccounting.dto.downpayment;

import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Running downpayment balance for one customer/supplier (or one downpayment) over a period. */
@Data
public class DownpaymentStatementResponse {
    private DownpaymentType type;
    private Long counterpartyId;
    private String counterpartyName;
    private Long downpaymentId;
    private String downpaymentNumber;
    private String currency;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal totalIncrease = BigDecimal.ZERO;
    private BigDecimal totalDecrease = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;
    private List<Line> lines = new ArrayList<>();

    @Data
    public static class Line {
        private LocalDate date;
        /** RECEIPT, APPLICATION, APPLICATION_REVERSAL, REFUND, REFUND_REVERSAL, REVERSAL. */
        private String entryType;
        private String entryLabel;
        private Long downpaymentId;
        private String downpaymentNumber;
        private String documentNumber;
        private String reference;
        private String description;
        private BigDecimal increase = BigDecimal.ZERO;
        private BigDecimal decrease = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
    }
}
