package com.unionsg.xaccounting.dto.downpayment;

import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DownpaymentResponse {
    private Long id;
    private String downpaymentNumber;
    private DownpaymentType type;
    private String typeLabel;
    private Long customerId;
    private Long supplierId;
    private Long counterpartyId;
    private String counterpartyName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String currency;
    private Long bankAccountId;
    private String bankAccountName;
    private String controlAccountCode;
    private String controlAccountName;
    private String reference;
    private String description;
    private BigDecimal appliedAmount;
    private BigDecimal refundedAmount;
    private BigDecimal availableBalance;
    private DownpaymentStatus status;
    private String statusLabel;
    private Long journalId;
    private String journalNumber;
    private Long reversalJournalId;
    private String reversalJournalNumber;
    private LocalDateTime postedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime reversedAt;
    private String reversalReason;
    private LocalDateTime createdAt;
    private CreatedByDTO createdBy;
    private List<DownpaymentAllocationResponse> allocations = new ArrayList<>();
    private List<DownpaymentRefundResponse> refunds = new ArrayList<>();
}
