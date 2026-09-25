package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.loan.LoanInterestType;
import com.unionsg.xaccounting.enums.loan.LoanRepaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateLoanRequest {

    private Long loanTypeId;

    private LoanDirection direction;

    private LoanCounterpartyType counterpartyType;

    /** Required for BANK/FINANCIAL_INSTITUTION/SHAREHOLDER/DIRECTOR/OTHER; auto-filled otherwise. */
    private String counterpartyName;

    private Long employeeId;

    private Long customerId;

    private Long supplierId;

    private BigDecimal principalAmount;

    private String currency;

    private BigDecimal interestRate;

    private LoanInterestType interestType;

    private LoanInterestMethod interestMethod;

    private LocalDate startDate;

    private LocalDate maturityDate;

    private LoanFrequency paymentFrequency;

    private Integer numberOfInstallments;

    private LoanRepaymentMethod repaymentMethod;

    private BigDecimal totalFees;

    private LoanFeeTreatment feeTreatment;

    private Long bankAccountId;

    private Long principalAccountId;

    private Long interestAccountId;

    private String notes;
}
