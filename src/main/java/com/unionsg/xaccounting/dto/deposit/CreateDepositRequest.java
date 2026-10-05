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
public class CreateDepositRequest {
    private DepositDirection direction;
    private Long depositTypeId;
    private DepositCounterpartyType counterpartyType;
    private Long customerId;
    private Long supplierId;
    private Long employeeId;
    /** Required for OTHER; ignored otherwise (taken from the linked record). */
    private String counterpartyName;
    private BigDecimal amount;
    private String currency;
    private LocalDate depositDate;
    private LocalDate expectedReturnDate;
    /** Defaults from the deposit type when null. */
    private Boolean refundable;
    private String purpose;
    private String reference;
    private Long bankAccountId;
    /** Chart of Accounts primary key; overrides the type's/default account when set. */
    private Long depositAccountId;
    /** Defaults from the deposit type when null. */
    private Boolean interestBearing;
    private BigDecimal interestRate;
    private LocalDate interestStartDate;
    private String interestTerms;
    private String notes;
}
