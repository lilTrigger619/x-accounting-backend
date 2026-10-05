package com.unionsg.xaccounting.dto.deposit;

import com.unionsg.xaccounting.enums.deposit.DepositCounterpartyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferDepositRequest {
    private BigDecimal amount;
    private LocalDate transferDate;
    /** Defaults to the source deposit's type. */
    private Long depositTypeId;
    private DepositCounterpartyType counterpartyType;
    private Long customerId;
    private Long supplierId;
    private Long employeeId;
    private String counterpartyName;
    /** Chart of Accounts primary key for the new deposit; defaults to the type's/default account. */
    private Long depositAccountId;
    private LocalDate expectedReturnDate;
    private String reference;
    private String notes;
}
