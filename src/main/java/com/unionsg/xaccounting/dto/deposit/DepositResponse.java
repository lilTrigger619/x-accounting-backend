package com.unionsg.xaccounting.dto.deposit;

import com.unionsg.xaccounting.enums.deposit.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositResponse {
    private Long id;
    private String depositNumber;
    private DepositDirection direction;
    private String directionLabel;
    private Long depositTypeId;
    private String depositTypeName;
    private DepositCounterpartyType counterpartyType;
    private String counterpartyTypeLabel;
    private String counterpartyName;
    private Long customerId;
    private Long supplierId;
    private Long employeeId;
    private BigDecimal amount;
    private String currency;
    private LocalDate depositDate;
    private LocalDate expectedReturnDate;
    private Boolean refundable;
    private String purpose;
    private String reference;
    private Long bankAccountId;
    private String bankAccountName;
    /** The account override stored on this deposit (null when it uses the type/default). */
    private Long depositAccountId;
    /** The account the deposit actually posts to, after type and mapping fallbacks. */
    private String effectiveAccountCode;
    private String effectiveAccountName;
    private DepositClassification classification;
    private String classificationLabel;
    private String accountingTreatment;
    private Boolean interestBearing;
    private BigDecimal interestRate;
    private LocalDate interestStartDate;
    private String interestTerms;
    /** Simple interest on the available balance from the start date to today (or until fully settled). */
    private BigDecimal estimatedInterest;
    private String notes;
    private DepositStatus status;
    private String statusLabel;
    private BigDecimal appliedAmount;
    private BigDecimal refundedAmount;
    private BigDecimal forfeitedAmount;
    private BigDecimal transferredAmount;
    private BigDecimal availableBalance;
    private Long journalId;
    private String journalNumber;
    private Long transferredFromId;
    private String transferredFromNumber;
    private LocalDateTime activatedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime reversedAt;
    private String reversalReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DepositAllocationResponse> allocations = new ArrayList<>();
}
