package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class LoanResponse {

    private Long id;
    private String loanNumber;
    private Long loanTypeId;
    private String loanTypeName;
    private LoanDirection direction;
    private LoanCounterpartyType counterpartyType;
    private String counterpartyName;
    private Long employeeId;
    private Long customerId;
    private Long supplierId;
    private BigDecimal principalAmount;
    private String currency;
    private BigDecimal interestRate;
    private LoanInterestMethod interestMethod;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private LoanFrequency paymentFrequency;
    private Integer numberOfInstallments;
    private Integer gracePeriodInstallments;
    private BigDecimal totalFees;
    private LoanFeeTreatment feeTreatment;
    private BigDecimal installmentFee;
    private Boolean allowOverpayment;

    private Long bankAccountId;
    private String bankAccountName;
    private Long principalAccountId;
    private String principalAccountCode;
    private String principalAccountName;
    private Long interestAccountId;
    private String interestAccountCode;
    private String interestAccountName;

    private String collateralDescription;
    private BigDecimal collateralValue;
    private String externalReference;
    private String notes;

    private LoanStatus status;
    private String statusReason;

    // Balances
    private BigDecimal outstandingPrincipal;
    /** Accrued to the GL and not yet paid. */
    private BigDecimal outstandingInterest;
    private BigDecimal interestDueToDate;
    private BigDecimal feesDueToDate;
    private BigDecimal overdueAmount;
    private BigDecimal totalOutstanding;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal feesPaid;
    private BigDecimal totalScheduledInterest;
    private BigDecimal overpaymentBalance;
    private BigDecimal writtenOffAmount;
    private LocalDate nextDueDate;
    private BigDecimal nextDueAmount;

    private Long journalId;
    private String journalNumber;
    private Long writeOffJournalId;
    private String writeOffJournalNumber;

    private LocalDateTime approvedAt;
    private LocalDateTime disbursedAt;
    private LocalDateTime defaultedAt;
    private LocalDateTime closedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime reversedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<LoanLineResponse> schedule;
}
