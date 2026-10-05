package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A payment against a loan. Send {@code amount} to have it allocated automatically (fees,
 * interest, then principal of installments due, then early principal), or any of
 * {@code principalAmount}/{@code interestAmount}/{@code feesAmount} to split it yourself.
 */
@Getter
@Setter
public class RecordLoanRepaymentRequest {

    private LocalDate repaymentDate;

    private BigDecimal amount;

    private BigDecimal principalAmount;

    private BigDecimal interestAmount;

    private BigDecimal feesAmount;

    private PaymentMethod paymentMethod;

    private Long bankAccountId;

    private String referenceNumber;

    private String memo;

    public boolean hasManualSplit() {
        return principalAmount != null || interestAmount != null || feesAmount != null;
    }
}
