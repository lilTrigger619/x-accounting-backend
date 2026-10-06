package com.unionsg.xaccounting.dto.downpayment;

import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Downpayment position of one customer or supplier, for their detail screen. */
@Data
public class CounterpartyDownpaymentSummary {
    private DownpaymentType type;
    private Long counterpartyId;
    private long count;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal appliedAmount = BigDecimal.ZERO;
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private BigDecimal availableBalance = BigDecimal.ZERO;
    private List<DownpaymentResponse> downpayments = new ArrayList<>();
}
