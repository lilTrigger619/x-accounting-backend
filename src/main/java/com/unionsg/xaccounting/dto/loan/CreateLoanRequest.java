package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Create or update (draft only) a loan. Also the body of the schedule preview. */
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

    /** A code from the "currencies" configuration; the default currency when blank. */
    private String currency;

    /** Annual rate, in percent. */
    private BigDecimal interestRate;

    private LoanInterestMethod interestMethod;

    private LocalDate startDate;

    /** Worked out from the schedule; only kept from the request when the schedule is custom and blank. */
    private LocalDate maturityDate;

    private LoanFrequency paymentFrequency;

    private Integer numberOfInstallments;

    private Integer gracePeriodInstallments;

    /** Upfront fees charged at disbursement. */
    private BigDecimal totalFees;

    private LoanFeeTreatment feeTreatment;

    /** Fee added to every installment. */
    private BigDecimal installmentFee;

    private Boolean allowOverpayment;

    private Long bankAccountId;

    /** Loan liability (borrowed) or loan receivable (lent). Falls back to Accounting Mappings. */
    private Long principalAccountId;

    /** Interest expense (borrowed) or interest income (lent). Falls back to Accounting Mappings. */
    private Long interestAccountId;

    private String collateralDescription;

    private BigDecimal collateralValue;

    private String externalReference;

    private String notes;

    /** Installments of a CUSTOM_SCHEDULE loan. */
    private List<CustomInstallmentRequest> customSchedule;
}
