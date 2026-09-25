package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecordLoanRepaymentRequest {

    private LocalDate repaymentDate;

    private BigDecimal principalAmount;

    private BigDecimal interestAmount;

    private BigDecimal feesAmount;

    private PaymentMethod paymentMethod;

    private Long bankAccountId;

    private String referenceNumber;

    private String memo;
}
