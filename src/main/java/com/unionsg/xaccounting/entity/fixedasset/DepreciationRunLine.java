package com.unionsg.xaccounting.entity.fixedasset;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationLineStatus;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One asset's charge in a depreciation run, with every input that produced it so the amount can be
 * explained later: the window charged, the opening carrying amount, residual value, remaining life,
 * the method, rate and convention, and a plain-language working.
 */
@Entity
@Table(name = "depreciation_run_lines")
@Getter
@Setter
public class DepreciationRunLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id")
    private DepreciationRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private FixedAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepreciationLineStatus status = DepreciationLineStatus.CALCULATED;

    /** First and last day this charge covers. Earlier than the period start for a catch-up. */
    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DepreciationMethod method;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DepreciationConvention convention;

    @Column(name = "opening_cost", precision = 19, scale = 2)
    private BigDecimal openingCost = BigDecimal.ZERO;

    @Column(name = "opening_accumulated_depreciation", precision = 19, scale = 2)
    private BigDecimal openingAccumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "opening_accumulated_impairment", precision = 19, scale = 2)
    private BigDecimal openingAccumulatedImpairment = BigDecimal.ZERO;

    @Column(name = "opening_carrying_amount", precision = 19, scale = 2)
    private BigDecimal openingCarryingAmount = BigDecimal.ZERO;

    @Column(name = "residual_value", precision = 19, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths;

    /** Months of life left at the start of the window (fractional under daily pro-rata). */
    @Column(name = "remaining_life_months", precision = 12, scale = 6)
    private BigDecimal remainingLifeMonths;

    /** Months of service charged in this window. */
    @Column(name = "months_charged", precision = 12, scale = 6)
    private BigDecimal monthsCharged;

    /** Annual rate used for reducing balance, as a percentage. */
    @Column(name = "annual_rate", precision = 9, scale = 4)
    private BigDecimal annualRate;

    @Column(name = "depreciation_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal depreciationAmount = BigDecimal.ZERO;

    @Column(name = "closing_accumulated_depreciation", precision = 19, scale = 2)
    private BigDecimal closingAccumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "closing_carrying_amount", precision = 19, scale = 2)
    private BigDecimal closingCarryingAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_account_id")
    private AccountEntity expenseAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accumulated_depreciation_account_id")
    private AccountEntity accumulatedDepreciationAccount;

    /** The asset's version when calculated; a different version at posting means it changed since. */
    @Column(name = "asset_version")
    private Long assetVersion;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    /** Why the line was skipped or is in error. */
    @Column(name = "message", length = 2000)
    private String message;
}
