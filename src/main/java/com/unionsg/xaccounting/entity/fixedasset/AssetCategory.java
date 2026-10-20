package com.unionsg.xaccounting.entity.fixedasset;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
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

/**
 * A class of assets (buildings, vehicles, computers) carrying the depreciation defaults and GL
 * accounts its assets inherit. An asset can override any account; whatever neither sets falls
 * back to the {@code MappingKey.FIXED_ASSET_*} mapping.
 */
@Entity
@Table(name = "asset_categories")
@Getter
@Setter
public class AssetCategory extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_method", nullable = false, length = 30)
    private DepreciationMethod defaultMethod = DepreciationMethod.STRAIGHT_LINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_convention", nullable = false, length = 30)
    private DepreciationConvention defaultConvention = DepreciationConvention.DAILY_PRORATA;

    @Column(name = "default_useful_life_months")
    private Integer defaultUsefulLifeMonths;

    /** Residual value as a percentage of cost, suggested on new assets. */
    @Column(name = "default_residual_rate", precision = 9, scale = 4)
    private BigDecimal defaultResidualRate;

    /** Annual rate (percent) for reducing balance, suggested on new assets. */
    @Column(name = "default_annual_rate", precision = 9, scale = 4)
    private BigDecimal defaultAnnualRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_account_id")
    private AccountEntity costAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accumulated_depreciation_account_id")
    private AccountEntity accumulatedDepreciationAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depreciation_expense_account_id")
    private AccountEntity depreciationExpenseAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accumulated_impairment_account_id")
    private AccountEntity accumulatedImpairmentAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impairment_loss_account_id")
    private AccountEntity impairmentLossAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_gain_account_id")
    private AccountEntity disposalGainAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_loss_account_id")
    private AccountEntity disposalLossAccount;

    @Column(nullable = false)
    private Boolean active = true;
}
