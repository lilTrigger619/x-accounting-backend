package com.unionsg.xaccounting.entity.fixedasset;

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
import java.time.LocalDate;

/**
 * An approved change to an active asset's useful life, residual value, method or rate. Applied
 * prospectively: it changes depreciation from the next unposted day onward and never touches what
 * was already posted.
 */
@Entity
@Table(name = "asset_estimate_changes")
@Getter
@Setter
public class AssetEstimateChange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private FixedAsset asset;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "previous_useful_life_months")
    private Integer previousUsefulLifeMonths;

    @Column(name = "new_useful_life_months")
    private Integer newUsefulLifeMonths;

    @Column(name = "previous_residual_value", precision = 19, scale = 2)
    private BigDecimal previousResidualValue;

    @Column(name = "new_residual_value", precision = 19, scale = 2)
    private BigDecimal newResidualValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_method", length = 30)
    private DepreciationMethod previousMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_method", length = 30)
    private DepreciationMethod newMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_convention", length = 30)
    private DepreciationConvention previousConvention;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_convention", length = 30)
    private DepreciationConvention newConvention;

    @Column(name = "previous_annual_rate", precision = 9, scale = 4)
    private BigDecimal previousAnnualRate;

    @Column(name = "new_annual_rate", precision = 9, scale = 4)
    private BigDecimal newAnnualRate;

    @Column(nullable = false, length = 1000)
    private String reason;
}
