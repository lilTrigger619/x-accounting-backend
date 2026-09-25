package com.unionsg.xaccounting.dto.prepayment;

import com.unionsg.xaccounting.enums.prepayment.PrepaymentCounterpartyType;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentFrequency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreatePrepaymentRequest {

    private Long prepaymentTypeId;

    private PrepaymentCounterpartyType counterpartyType;

    /** Required for OTHER; auto-filled from the linked supplier/employee otherwise. */
    private String counterpartyName;

    private Long supplierId;

    private Long employeeId;

    private BigDecimal totalAmount;

    private String currency;

    private LocalDate paymentDate;

    private LocalDate recognitionStartDate;

    private Integer numberOfPeriods;

    private PrepaymentFrequency recognitionFrequency;

    private Long prepaidAccountId;

    private Long expenseAccountId;

    private Long bankAccountId;

    private String notes;
}
