package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.loan.LoanInterestType;
import com.unionsg.xaccounting.enums.loan.LoanRepaymentMethod;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LoanResponse {

    private Long id;
    private String loanNumber;
    private Long loanTypeId;
    private String loanTypeName;
    private LoanDirection direction;
    private LoanCounterpartyType counterpartyType;
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
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private BigDecimal totalFees;
    private LoanFeeTreatment feeTreatment;
    private LoanStatus status;
    private String notes;
    private List<LoanLineResponse> schedule;
}
