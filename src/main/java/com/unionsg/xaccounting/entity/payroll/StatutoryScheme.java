package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.StatutoryCalculationBasis;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A jurisdiction-specific statutory contribution scheme (social security, pension, etc.), with
 * the employee and employer rates and their accounting mappings versioned by effective date
 * (§10, §11) - a rate change never rewrites the calculation of a payroll run already posted for
 * an earlier period.
 */
@Entity
@Table(name = "statutory_schemes")
@Getter
@Setter
public class StatutoryScheme extends BaseEntity {

    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    private StatutoryCalculationBasis calculationBasis;

    /** Null if employees do not contribute to this scheme. */
    private BigDecimal employeeRatePercent;

    /** Null if the employer does not contribute to this scheme. */
    private BigDecimal employerRatePercent;

    /** Liability credited when the employee's portion is withheld. */
    private String employeeLiabilityAccountCode;

    /** Expense debited for the employer's own cost. */
    private String employerExpenseAccountCode;

    /** Liability credited for the employer's portion, payable to the scheme. */
    private String employerLiabilityAccountCode;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private boolean active = true;

    private String description;
}
