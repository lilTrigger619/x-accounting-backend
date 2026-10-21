package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.dto.fixedasset.ChangeEstimateRequest;
import com.unionsg.xaccounting.dto.fixedasset.CreateDepreciationRunRequest;
import com.unionsg.xaccounting.dto.fixedasset.DisposeAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.ImpairAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.PeriodDepreciationStatusResponse;
import com.unionsg.xaccounting.dto.fixedasset.SaveAssetCategoryRequest;
import com.unionsg.xaccounting.dto.fixedasset.SaveFixedAssetRequest;
import com.unionsg.xaccounting.entity.payroll.Department;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.fixedasset.AssetAcquisitionSource;
import com.unionsg.xaccounting.enums.fixedasset.AssetDisposalType;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.fixedasset.AssetCategoryRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetRepository;
import com.unionsg.xaccounting.repository.payroll.DepartmentRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.fixedasset.AssetCategoryService;
import com.unionsg.xaccounting.service.fixedasset.DepreciationRunService;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds a working fixed asset register so the Fixed Assets screens, the nine fixed asset reports,
 * the Balance Sheet PP&amp;E note and the Cash Flow Statement have real movements to show.
 *
 * <p>Five categories (one per depreciation method and convention the engine supports) and seven
 * assets, acquired three different ways: paid from a bank account, financed through a long-term
 * liability, and contributed by the owner. Everything goes through the module's own services, so
 * each acquisition, depreciation run, impairment and disposal posts a real journal exactly as a
 * user would, and the register stays reconciled to the general ledger.</p>
 *
 * <p>The story, all inside FY2026 and after the two periods {@link DemoDataSeeder} locks:</p>
 * <ul>
 *   <li>March to May: six assets are bought and activated, and a seventh is left in draft.</li>
 *   <li>March to July: a depreciation run is calculated, submitted, approved and posted each month.</li>
 *   <li>End of July: the generator is impaired after storm damage, and the laptops' useful life
 *       is shortened to reflect a faster refresh cycle.</li>
 *   <li>August: the server rack is sold at a loss (catch-up depreciation to the sale date is
 *       charged first), then August's run is posted.</li>
 *   <li>September: the run is calculated and submitted, so the approval queue isn't empty.</li>
 * </ul>
 *
 * <p>Runs after {@link NewModulesDemoSeeder} (the KES bank accounts) and
 * {@link FY2025FullDemoSeeder}. It is idempotent: it does nothing once any asset category
 * exists. Like the rest of this package, a failure is logged and rolled back without stopping
 * the application from starting.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10)
public class FixedAssetDemoSeeder implements ApplicationRunner {

    private static final String ACCUMULATED_DEPRECIATION = "1710";
    private static final String ACCUMULATED_IMPAIRMENT = "1715";
    private static final String DEPRECIATION_EXPENSE = "5120";
    private static final String IMPAIRMENT_LOSS = "5130";
    private static final String DISPOSAL_GAIN = "4070";
    private static final String DISPOSAL_LOSS = "5125";
    private static final String LONG_TERM_LIABILITY = "2000";
    private static final String OWNERS_EQUITY = "3000";

    private final AssetCategoryRepository categoryRepository;
    private final FixedAssetRepository assetRepository;
    private final AccountRepository accountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SupplierRepository supplierRepository;
    private final DepartmentRepository departmentRepository;

    private final AssetCategoryService categoryService;
    private final FixedAssetService assetService;
    private final DepreciationRunService runService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            if (categoryRepository.count() > 0 || assetRepository.count() > 0) {
                log.info("Fixed asset data already present - skipping FixedAssetDemoSeeder.");
                return;
            }

            Long it = category("IT", "Computers & IT Equipment", "1610", DepreciationMethod.STRAIGHT_LINE,
                    DepreciationConvention.FULL_MONTH, 36, "0", null);
            Long vehicles = category("VEH", "Motor Vehicles", "1600", DepreciationMethod.REDUCING_BALANCE,
                    DepreciationConvention.FULL_MONTH, 60, "10", "25");
            Long furniture = category("FUR", "Furniture & Fixtures", "1530", DepreciationMethod.STRAIGHT_LINE,
                    DepreciationConvention.MID_MONTH, 84, "0", null);
            Long machinery = category("MCH", "Machinery & Equipment", "1520", DepreciationMethod.STRAIGHT_LINE,
                    DepreciationConvention.DAILY_PRORATA, 120, "5", null);
            Long leasehold = category("LHI", "Leasehold Improvements", "1630", DepreciationMethod.STRAIGHT_LINE,
                    DepreciationConvention.FULL_MONTH, 120, "0", null);

            Long operating = bankAccount("KES Operating Account");
            Long reserve = bankAccount("KES Reserve Account");
            Long finance = department("Finance");
            Long engineering = department("Engineering");
            Long atlasOffice = supplier("Atlas Office Supply Co.");
            Long falconRentals = supplier("Falcon Equipment Rentals");

            Long fitOut = asset("Head office fit-out", leasehold, "Partitioning, wiring and lighting for the 3rd floor office",
                    null, finance, null, "2026-03-01", "24000.00", "0", 120, DepreciationMethod.STRAIGHT_LINE,
                    DepreciationConvention.FULL_MONTH, null, AssetAcquisitionSource.OFFSET_ACCOUNT, null, OWNERS_EQUITY);
            Long laptops = asset("Dell Latitude laptops (x6)", it, "Finance and engineering staff laptops",
                    "DL-5540-2026-A", engineering, atlasOffice, "2026-03-02", "4800.00", "0", 36,
                    DepreciationMethod.STRAIGHT_LINE, DepreciationConvention.FULL_MONTH, null,
                    AssetAcquisitionSource.BANK_PAYMENT, operating, null);
            Long serverRack = asset("Server rack and NAS", it, "On-premise file server, replaced by cloud hosting in August",
                    "SRV-NAS-0912", engineering, atlasOffice, "2026-03-02", "3600.00", "0", 36,
                    DepreciationMethod.STRAIGHT_LINE, DepreciationConvention.FULL_MONTH, null,
                    AssetAcquisitionSource.BANK_PAYMENT, operating, null);
            Long pickup = asset("Toyota Hilux double cab", vehicles, "Field operations vehicle, financed over 4 years",
                    "KDQ 512X", engineering, null, "2026-03-16", "18000.00", "1800.00", 60,
                    DepreciationMethod.REDUCING_BALANCE, DepreciationConvention.FULL_MONTH, "25",
                    AssetAcquisitionSource.OFFSET_ACCOUNT, null, LONG_TERM_LIABILITY);
            Long desks = asset("Open-plan workstations", furniture, "Twelve desks with pedestals and chairs",
                    null, finance, atlasOffice, "2026-04-06", "2940.00", "0", 84,
                    DepreciationMethod.STRAIGHT_LINE, DepreciationConvention.MID_MONTH, null,
                    AssetAcquisitionSource.BANK_PAYMENT, reserve, null);
            Long generator = asset("Standby diesel generator 60 kVA", machinery, "Backup power for the head office",
                    "GEN-60K-77341", engineering, falconRentals, "2026-05-18", "6000.00", "300.00", 120,
                    DepreciationMethod.STRAIGHT_LINE, DepreciationConvention.DAILY_PRORATA, null,
                    AssetAcquisitionSource.BANK_PAYMENT, operating, null);
            asset("Boardroom AV system", it, "Display, conference camera and speakers - awaiting installation",
                    null, finance, atlasOffice, "2026-09-22", "2100.00", "0", 36,
                    DepreciationMethod.STRAIGHT_LINE, DepreciationConvention.FULL_MONTH, null,
                    AssetAcquisitionSource.BANK_PAYMENT, operating, null);

            for (Long id : new Long[]{fitOut, laptops, serverRack, pickup, desks, generator}) {
                assetService.activate(id);
            }

            for (int month = 3; month <= 7; month++) {
                postRun(month);
            }

            ImpairAssetRequest impairment = new ImpairAssetRequest();
            impairment.setImpairmentDate(LocalDate.parse("2026-07-31"));
            impairment.setAmount(new BigDecimal("500.00"));
            impairment.setReason("Alternator damaged in the July storm; repair quote exceeds the cost of replacement parts.");
            assetService.impair(generator, impairment);

            ChangeEstimateRequest shorterLife = new ChangeEstimateRequest();
            shorterLife.setUsefulLifeMonths(30);
            shorterLife.setReason("Laptop refresh cycle shortened from 3 years to 30 months.");
            assetService.changeEstimate(laptops, shorterLife);

            DisposeAssetRequest sale = new DisposeAssetRequest();
            sale.setDisposalType(AssetDisposalType.SALE);
            sale.setDisposalDate(LocalDate.parse("2026-08-14"));
            sale.setProceeds(new BigDecimal("2600.00"));
            sale.setBankAccountId(operating);
            sale.setReason("Sold to a staff member after moving file storage to cloud hosting.");
            assetService.dispose(serverRack, sale);

            postRun(8);

            Long september = runService.calculate(runRequest(9, "September depreciation, waiting for approval.")).getId();
            runService.submit(september);

            log.info("Fixed asset demo seeding complete: 5 categories, 7 assets, depreciation posted March to "
                    + "August 2026, one impairment, one estimate change, one sale, and September awaiting approval.");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("FixedAssetDemoSeeder failed during startup seeding - skipping so the "
                    + "application can still start. Any changes from this run were rolled back.", e);
        }
    }

    // ================= Depreciation runs =================

    /** Calculates, submits, approves and posts the given month's run, as the full workflow would. */
    private void postRun(int month) {
        Long id = runService.calculate(runRequest(month, null)).getId();
        runService.submit(id);
        runService.approve(id);
        runService.post(id);
    }

    private CreateDepreciationRunRequest runRequest(int month, String notes) {
        LocalDate start = LocalDate.of(2026, month, 1);
        PeriodDepreciationStatusResponse period = runService.periods().stream()
                .filter(p -> start.equals(p.getStartDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No accounting period starts on " + start
                        + " - DemoDataSeeder should have created FY2026"));
        CreateDepreciationRunRequest request = new CreateDepreciationRunRequest();
        request.setAccountingPeriodId(period.getPeriodId());
        request.setNotes(notes);
        return request;
    }

    // ================= Categories and assets =================

    private Long category(String code, String name, String costAccount, DepreciationMethod method,
                          DepreciationConvention convention, int lifeMonths, String residualRate, String annualRate) {
        SaveAssetCategoryRequest r = new SaveAssetCategoryRequest();
        r.setCode(code);
        r.setName(name);
        r.setDefaultMethod(method);
        r.setDefaultConvention(convention);
        r.setDefaultUsefulLifeMonths(lifeMonths);
        r.setDefaultResidualRate(new BigDecimal(residualRate));
        r.setDefaultAnnualRate(annualRate == null ? null : new BigDecimal(annualRate));
        r.setCostAccountId(accountId(costAccount));
        r.setAccumulatedDepreciationAccountId(accountId(ACCUMULATED_DEPRECIATION));
        r.setDepreciationExpenseAccountId(accountId(DEPRECIATION_EXPENSE));
        r.setAccumulatedImpairmentAccountId(accountId(ACCUMULATED_IMPAIRMENT));
        r.setImpairmentLossAccountId(accountId(IMPAIRMENT_LOSS));
        r.setDisposalGainAccountId(accountId(DISPOSAL_GAIN));
        r.setDisposalLossAccountId(accountId(DISPOSAL_LOSS));
        r.setActive(true);
        return categoryService.create(r).getId();
    }

    private Long asset(String name, Long categoryId, String description, String serialNumber, Long departmentId,
                       Long supplierId, String acquired, String cost, String residual, int lifeMonths,
                       DepreciationMethod method, DepreciationConvention convention, String annualRate,
                       AssetAcquisitionSource source, Long bankAccountId, String offsetAccount) {
        SaveFixedAssetRequest r = new SaveFixedAssetRequest();
        r.setName(name);
        r.setCategoryId(categoryId);
        r.setDescription(description);
        r.setSerialNumber(serialNumber);
        r.setDepartmentId(departmentId);
        r.setSupplierId(supplierId);
        r.setAcquisitionDate(LocalDate.parse(acquired));
        r.setAvailableForUseDate(LocalDate.parse(acquired));
        r.setAcquisitionCost(new BigDecimal(cost));
        r.setResidualValue(new BigDecimal(residual));
        r.setUsefulLifeMonths(lifeMonths);
        r.setDepreciationMethod(method);
        r.setDepreciationConvention(convention);
        r.setAnnualRate(annualRate == null ? null : new BigDecimal(annualRate));
        r.setAcquisitionSource(source);
        r.setBankAccountId(bankAccountId);
        r.setOffsetAccountId(offsetAccount == null ? null : accountId(offsetAccount));
        r.setOpeningAccumulatedDepreciation(BigDecimal.ZERO);
        return assetService.create(r).getId();
    }

    // ================= Lookups of other seeders' data =================

    private Long accountId(String code) {
        return accountRepository.findByAccountId(code)
                .orElseThrow(() -> new IllegalStateException("Chart of accounts entry not found: " + code))
                .getId();
    }

    private Long bankAccount(String accountName) {
        return bankAccountRepository.findAll().stream()
                .filter(a -> accountName.equals(a.getAccountName()))
                .map(BankAccount::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo bank account not found: " + accountName
                        + " - NewModulesDemoSeeder should have seeded it already"));
    }

    /** Departments are optional on an asset, so a missing one simply leaves the field blank. */
    private Long department(String name) {
        return departmentRepository.findAll().stream()
                .filter(d -> name.equals(d.getName()))
                .map(Department::getId)
                .findFirst()
                .orElse(null);
    }

    /** Suppliers are optional on an asset, so a missing one simply leaves the field blank. */
    private Long supplier(String displayName) {
        return supplierRepository.findAll().stream()
                .filter(s -> displayName.equals(s.getDisplayName()))
                .map(Supplier::getId)
                .findFirst()
                .orElse(null);
    }
}
