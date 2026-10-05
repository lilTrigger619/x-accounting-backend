package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.loan.LoanPaymentStatus;
import com.unionsg.xaccounting.enums.loan.LoanPaymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class LoanRepaymentResponse {
    private Long id;
    private LocalDate repaymentDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal feesAmount;
    private BigDecimal overpaymentAmount;
    private BigDecimal totalAmount;
    private LoanPaymentType paymentType;
    private LoanPaymentStatus status;
    private PaymentMethod paymentMethod;
    private Long bankAccountId;
    private String bankAccountName;
    private String referenceNumber;
    private String memo;
    private Long journalId;
    private String journalNumber;
    private Long reversalJournalId;
    private String reversalJournalNumber;
    private LocalDateTime reversedAt;
    private String reversalReason;
    private LocalDateTime createdAt;
}
