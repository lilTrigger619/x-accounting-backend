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
public class DepositListItemResponse {
    private Long id;
    private String depositNumber;
    private DepositDirection direction;
    private String directionLabel;
    private Long depositTypeId;
    private String depositTypeName;
    private DepositCounterpartyType counterpartyType;
    private String counterpartyName;
    private Long customerId;
    private Long supplierId;
    private Long employeeId;
    private BigDecimal amount;
    private String currency;
    private LocalDate depositDate;
    private LocalDate expectedReturnDate;
    private Boolean refundable;
    private String reference;
    private DepositStatus status;
    private String statusLabel;
    private DepositClassification classification;
    private String classificationLabel;
    private BigDecimal appliedAmount;
    private BigDecimal refundedAmount;
    private BigDecimal forfeitedAmount;
    private BigDecimal transferredAmount;
    private BigDecimal availableBalance;
    private LocalDateTime createdAt;
}
