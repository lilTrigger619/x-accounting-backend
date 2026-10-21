package com.unionsg.xaccounting.entity.fixedasset;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.Department;
import com.unionsg.xaccounting.entity.payroll.WorkLocation;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.fixedasset.AssetAcquisitionSource;
import com.unionsg.xaccounting.enums.fixedasset.AssetDisposalType;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
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
import java.time.LocalDateTime;

/**
 * One asset in the fixed asset register.
 *
 * <p>The account fields are overrides while the asset is a draft. Activation resolves every account
 * (asset, then category, then mapping) and stores the result here, so a later change to the category
 * or mapping never moves an asset that is already on the books.</p>
 *
 * <p>The cost, accumulated depreciation and impairment totals are a cache of the asset's posted
 * {@link AssetTransaction} rows, refreshed after every posting.</p>
 */
@Entity
@Table(name = "fixed_assets")
@Getter
@Setter
public class FixedAsset extends BaseEntity {

    @Column(name = "asset_number", nullable = false, length = 50)
    private String assetNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private AssetCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WorkLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "available_for_use_date")
    private LocalDate availableForUseDate;

    /** Defaults to the date available for use; may be later, never earlier. */
    @Column(name = "depreciation_start_date")
    private LocalDate depreciationStartDate;

    @Column(name = "acquisition_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(name = "residual_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "depreciation_method", nullable = false, length = 30)
    private DepreciationMethod depreciationMethod = DepreciationMethod.STRAIGHT_LINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "depreciation_convention", nullable = false, length = 30)
    private DepreciationConvention depreciationConvention = DepreciationConvention.DAILY_PRORATA;

    /** Annual reducing-balance rate in percent. Derived from cost, residual and life when blank. */
    @Column(name = "annual_rate", precision = 9, scale = 4)
    private BigDecimal annualRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_source", nullable = false, length = 30)
    private AssetAcquisitionSource acquisitionSource = AssetAcquisitionSource.BANK_PAYMENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    /** Credited for an OFFSET_ACCOUNT acquisition. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offset_account_id")
    private AccountEntity offsetAccount;

    /** For ALREADY_IN_LEDGER: the posted journal that already carries the cost. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_journal_id")
    private JournalEntry sourceJournal;

    /** Depreciation already charged before the asset was brought onto this register. */
    @Column(name = "opening_accumulated_depreciation", precision = 19, scale = 2)
    private BigDecimal openingAccumulatedDepreciation = BigDecimal.ZERO;

    /** The date the brought-forward depreciation runs to. */
    @Column(name = "opening_depreciation_date")
    private LocalDate openingDepreciationDate;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FixedAssetStatus status = FixedAssetStatus.DRAFT;

    // ---- Posted totals (cache of the asset's transactions) ----

    @Column(name = "cost_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal costBalance = BigDecimal.ZERO;

    @Column(name = "accumulated_depreciation", nullable = false, precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "accumulated_impairment", nullable = false, precision = 19, scale = 2)
    private BigDecimal accumulatedImpairment = BigDecimal.ZERO;

    /** The last day posted depreciation covers. Null until the first charge. */
    @Column(name = "depreciated_through")
    private LocalDate depreciatedThrough;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquisition_journal_id")
    private JournalEntry acquisitionJournal;

    // ---- Disposal ----

    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_type", length = 30)
    private AssetDisposalType disposalType;

    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    @Column(name = "disposal_proceeds", precision = 19, scale = 2)
    private BigDecimal disposalProceeds;

    @Column(name = "disposal_gain_loss", precision = 19, scale = 2)
    private BigDecimal disposalGainLoss;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_journal_id")
    private JournalEntry disposalJournal;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime activatedAt;

    private LocalDateTime disposedAt;

    public BigDecimal getCarryingAmount() {
        return nz(costBalance).subtract(nz(accumulatedDepreciation)).subtract(nz(accumulatedImpairment));
    }

    public BigDecimal getOpeningAccumulatedDepreciation() {
        return openingAccumulatedDepreciation != null ? openingAccumulatedDepreciation : BigDecimal.ZERO;
    }

    /** The first day depreciation may be charged: the start date, else the date available for use. */
    public LocalDate getEffectiveDepreciationStart() {
        return depreciationStartDate != null ? depreciationStartDate : availableForUseDate;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
