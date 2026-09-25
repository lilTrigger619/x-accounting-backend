package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LoanRepaymentResponse {
    private Long id;
    private LocalDate repaymentDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal feesAmount;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String memo;
}
