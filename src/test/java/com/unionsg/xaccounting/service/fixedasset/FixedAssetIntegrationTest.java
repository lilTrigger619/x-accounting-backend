package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.dto.fixedasset.AssetActionRequest;
import com.unionsg.xaccounting.dto.fixedasset.AssetCategoryResponse;
import com.unionsg.xaccounting.dto.fixedasset.AssetTransactionResponse;
import com.unionsg.xaccounting.dto.fixedasset.ChangeEstimateRequest;
import com.unionsg.xaccounting.dto.fixedasset.CreateDepreciationRunRequest;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunActionRequest;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunLineResponse;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunResponse;
import com.unionsg.xaccounting.dto.fixedasset.DisposalPreviewResponse;
import com.unionsg.xaccounting.dto.fixedasset.DisposeAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetResponse;
import com.unionsg.xaccounting.dto.fixedasset.ImpairAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.SaveAssetCategoryRequest;
import com.unionsg.xaccounting.dto.fixedasset.SaveFixedAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.ScheduleLineResponse;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.ReconciliationReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.ReconciliationRow;
import com.unionsg.xaccounting.dto.journal.JournalLineResponse;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.reports.BalanceSheetAccountDto;
import com.unionsg.xaccounting.dto.reports.BalanceSheetResponseDTO;
import com.unionsg.xaccounting.dto.reports.TrialBalanceRowDto;
import com.unionsg.xaccounting.dto.reports.cashflow.CashFlowStatementResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.fixedasset.AssetAcquisitionSource;
import com.unionsg.xaccounting.enums.fixedasset.AssetDisposalType;
import com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.reports.BalanceSheetService;
import com.unionsg.xaccounting.service.reports.CashFlowStatementService;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import com.unionsg.xaccounting.service.reports.TrialBalanceService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The fixed asset lifecycle against the real database and the shared journal, period and report
 * services (same Postgres requirement as {@code XaccountingApplicationTests}). Each test rolls back.
 * The category posts to accounts created for the test, so the GL balances checked here belong to
 * the test's assets alone; the bank side uses the Fixed Assets bank mapping and is checked as a
 * before/after difference.
 */
@SpringBootTest
@Transactional
class FixedAssetIntegrationTest {

    private static final LocalDate APRIL_1 = LocalDate.of(2026, 4, 1);
    private static final LocalDate APRIL_30 = LocalDate.of(2026, 4, 30);
    private static final LocalDate MAY_31 = LocalDate.of(2026, 5, 31);
    private static final LocalDate JUNE_30 = LocalDate.of(2026, 6, 30);

    @Autowired private FixedAssetService assets;
    @Autowired private AssetCategoryService categories;
    @Autowired private DepreciationRunService runs;
    @Autowired private FixedAssetReportService reports;
    @Autowired private JournalService journalService;
    @Autowired private BalanceSheetService balanceSheetService;
    @Autowired private ProfitAndLossService profitAndLossService;
    @Autowired private TrialBalanceService trialBalanceService;
    @Autowired private CashFlowStatementService cashFlowService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountingPeriodRepository periodRepository;
    @Autowired private EntityManager entityManager;

    private AccountEntity cost;
    private AccountEntity accumulated;
    private AccountEntity expense;
    private AccountEntity impairment;
    private AccountEntity impairmentLoss;
    private AccountEntity gain;
    private AccountEntity loss;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        cost = clone("1730", "97301", "Test Equipment at Cost");
        accumulated = clone("1710", "97101", "Test Equipment Accumulated Depreciation");
        expense = clone("5120", "95121", "Test Depreciation Expense");
        impairment = clone("1715", "97151", "Test Accumulated Impairment");
        impairmentLoss = clone("5130", "95131", "Test Impairment Loss");
        gain = clone("4070", "94071", "Test Gain on Disposal");
        loss = clone("5125", "95126", "Test Loss on Disposal");
        for (AccountingPeriod p : periodRepository.findAll()) {
            if (p.getStartDate().getYear() == 2026 && p.getStartDate().getMonthValue() >= 3) p.setStatus(AccountingPeriodStatus.OPEN);
        }
        // Set aside whatever FixedAssetDemoSeeder (or earlier use) left, so each period is free to run
        // and only the test's own assets are depreciated. Rolled back with the test.
        entityManager.createNativeQuery("update depreciation_runs set active_period_id = null").executeUpdate();
        entityManager.createNativeQuery("update fixed_assets set is_deleted = true").executeUpdate();

        SaveAssetCategoryRequest c = new SaveAssetCategoryRequest();
        c.setCode("TEST-EQ");
        c.setName("Test Equipment");
        c.setDefaultMethod(DepreciationMethod.STRAIGHT_LINE);
        c.setDefaultConvention(DepreciationConvention.DAILY_PRORATA);
        c.setDefaultUsefulLifeMonths(12);
        c.setCostAccountId(cost.getId());
        c.setAccumulatedDepreciationAccountId(accumulated.getId());
        c.setDepreciationExpenseAccountId(expense.getId());
        c.setAccumulatedImpairmentAccountId(impairment.getId());
        c.setImpairmentLossAccountId(impairmentLoss.getId());
        c.setDisposalGainAccountId(gain.getId());
        c.setDisposalLossAccountId(loss.getId());
        AssetCategoryResponse category = categories.create(c);
        categoryId = category.getId();
        assertThat(category.getAccounts()).allSatisfy(a -> assertThat(a.getError()).isNull());
    }

    // ------------------------------------------------------------------
    // Accounts and acquisition
    // ------------------------------------------------------------------

    @Test
    void categoriesRefuseAccountsOfTheWrongClassification() {
        SaveAssetCategoryRequest c = new SaveAssetCategoryRequest();
        c.setCode("BAD");
        c.setName("Bad mapping");
        c.setDefaultUsefulLifeMonths(12);
        c.setDepreciationExpenseAccountId(cost.getId());

        assertThatThrownBy(() -> categories.create(c))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be an expense account");
    }

    @Test
    void activationPostsABalancedAcquisitionJournal() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());

        assertThat(asset.getStatus()).isEqualTo(FixedAssetStatus.ACTIVE);
        assertThat(asset.getCostBalance()).isEqualByComparingTo("12000");
        JournalResponse journal = journalService.getById(asset.getAcquisitionJournalId());
        assertBalanced(journal);
        assertThat(journal.getJournalType()).isEqualTo(JournalType.FIXED_ASSET);
        assertThat(debitOn(journal, cost)).isEqualByComparingTo("12000");
        assertThat(journal.getTotalCredit()).isEqualByComparingTo("12000");
    }

    // ------------------------------------------------------------------
    // Depreciation runs
    // ------------------------------------------------------------------

    @Test
    void aPostedRunPostsOneBalancedJournalDebitingExpenseAndCreditingAccumulatedDepreciation() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());

        DepreciationRunResponse run = postRun(APRIL_30);

        assertThat(run.getStatus()).isEqualTo(DepreciationRunStatus.POSTED);
        DepreciationRunLineResponse line = lineFor(run, asset.getId());
        assertThat(line.getDepreciationAmount()).isEqualByComparingTo("1000.00");
        assertThat(line.getExplanation()).contains("Straight line");
        JournalResponse journal = journalService.getById(run.getJournalId());
        assertBalanced(journal);
        assertThat(journal.getJournalType()).isEqualTo(JournalType.DEPRECIATION);
        assertThat(journal.getJournalDate()).isEqualTo(APRIL_30);
        assertThat(debitOn(journal, expense)).isEqualByComparingTo("1000.00");
        assertThat(creditOn(journal, accumulated)).isEqualByComparingTo("1000.00");

        FixedAssetResponse after = assets.get(asset.getId());
        assertThat(after.getAccumulatedDepreciation()).isEqualByComparingTo("1000.00");
        assertThat(after.getCarryingAmount()).isEqualByComparingTo("11000.00");
        assertThat(after.getDepreciatedThrough()).isEqualTo(APRIL_30);
        assertThat(assets.transactions(asset.getId())).filteredOn(t -> t.getType() == AssetTransactionType.DEPRECIATION)
                .singleElement().satisfies(t -> assertThat(t.getJournalId()).isEqualTo(run.getJournalId()));
    }

    @Test
    void aPeriodIsDepreciatedOnceAndTheNextRunChargesOnlyTheNewPeriod() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        DepreciationRunResponse april = postRun(APRIL_30);

        assertThatThrownBy(() -> runs.calculate(runRequest(APRIL_30)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has depreciation run");
        assertThatThrownBy(() -> runs.post(april.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only runs that are approved can be posted");

        DepreciationRunResponse may = postRun(MAY_31);
        DepreciationRunLineResponse line = lineFor(may, asset.getId());
        assertThat(line.getFromDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(line.getDepreciationAmount()).isEqualByComparingTo("1000.00");
        assertThat(assets.get(asset.getId()).getAccumulatedDepreciation()).isEqualByComparingTo("2000.00");
    }

    @Test
    void aMissedPeriodIsCaughtUpInTheNextRun() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());

        DepreciationRunResponse may = postRun(MAY_31);

        DepreciationRunLineResponse line = lineFor(may, asset.getId());
        assertThat(line.getFromDate()).isEqualTo(APRIL_1);
        assertThat(line.getDepreciationAmount()).isEqualByComparingTo("2000.00");
        assertThat(may.getWarnings()).anySatisfy(w -> assertThat(w).contains("catch-up"));
    }

    @Test
    void depreciationStopsAtResidualValue() {
        FixedAssetResponse asset = assets.activate(draft("3000", "1000", 2).getId());

        postRun(APRIL_30);
        postRun(MAY_31);
        FixedAssetResponse done = assets.get(asset.getId());
        assertThat(done.getCarryingAmount()).isEqualByComparingTo("1000.00");
        assertThat(done.getStatus()).isEqualTo(FixedAssetStatus.FULLY_DEPRECIATED);

        DepreciationRunResponse june = runs.calculate(runRequest(JUNE_30));
        assertThat(june.getLines()).noneMatch(l -> l.getAssetId().equals(asset.getId()));
    }

    @Test
    void lockedOrClosedPeriodsCanNotBeCalculatedOrPosted() {
        assets.activate(draft("12000", "0", 12).getId());
        DepreciationRunResponse april = runs.approve(runs.submit(runs.calculate(runRequest(APRIL_30)).getId()).getId());

        setPeriodStatus(APRIL_30, AccountingPeriodStatus.LOCKED);
        assertThatThrownBy(() -> runs.post(april.getId())).isInstanceOf(RuntimeException.class);

        setPeriodStatus(MAY_31, AccountingPeriodStatus.CLOSED);
        assertThatThrownBy(() -> runs.calculate(runRequest(MAY_31))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void aRunIsRecalculatedBeforePostingAndRefusedWhenTheAssetChanged() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        DepreciationRunResponse april = runs.approve(runs.submit(runs.calculate(runRequest(APRIL_30)).getId()).getId());

        ChangeEstimateRequest change = new ChangeEstimateRequest();
        change.setUsefulLifeMonths(6);
        change.setReason("Heavier use than planned");
        assets.changeEstimate(asset.getId(), change);

        assertThatThrownBy(() -> runs.post(april.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("out of date");
        DepreciationRunResponse recalculated = runs.recalculate(april.getId());
        assertThat(lineFor(recalculated, asset.getId()).getDepreciationAmount()).isEqualByComparingTo("2000.00");
        assertThat(recalculated.getStatus()).isEqualTo(DepreciationRunStatus.CALCULATED);
    }

    @Test
    void anImpairmentReducesLaterDepreciationProspectively() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        postRun(APRIL_30);

        ImpairAssetRequest impair = new ImpairAssetRequest();
        impair.setImpairmentDate(APRIL_30);
        impair.setAmount(new BigDecimal("1100"));
        impair.setReason("Damaged");
        FixedAssetResponse impaired = assets.impair(asset.getId(), impair);
        assertThat(impaired.getCarryingAmount()).isEqualByComparingTo("9900.00");

        // (12,000 - 1,000 - 1,100) / 11 months left
        assertThat(lineFor(postRun(MAY_31), asset.getId()).getDepreciationAmount()).isEqualByComparingTo("900.00");
    }

    // ------------------------------------------------------------------
    // Financial statements
    // ------------------------------------------------------------------

    @Test
    void depreciationReducesTheBalanceSheetAndProfitOnceAndTheNoteReconciles() {
        assets.activate(draft("12000", "0", 12).getId());
        BalanceSheetResponseDTO before = balanceSheetService.generateReport(APRIL_30);
        BigDecimal expensesBefore = profitAndLossService.generateReport(APRIL_1, APRIL_30).totalExpenses();

        postRun(APRIL_30);

        BalanceSheetResponseDTO after = balanceSheetService.generateReport(APRIL_30);
        assertThat(after.totalAssets().subtract(before.totalAssets())).isEqualByComparingTo("-1000.00");
        assertThat(after.currentYearEarnings().subtract(before.currentYearEarnings())).isEqualByComparingTo("-1000.00");
        assertThat(after.balanceCheck()).isEqualByComparingTo(before.balanceCheck());
        assertThat(balance(after.assets(), cost)).isEqualByComparingTo("12000.00");
        assertThat(balance(after.assets(), accumulated)).isEqualByComparingTo("-1000.00");

        assertThat(after.fixedAssetNote()).isNotNull();
        assertThat(after.fixedAssetNote().costLines()).anySatisfy(l -> {
            assertThat(l.accountCode()).isEqualTo(cost.getAccountId());
            assertThat(l.amount()).isEqualByComparingTo("12000.00");
        });
        assertThat(after.fixedAssetNote().accumulatedDepreciationLines()).anySatisfy(l -> {
            assertThat(l.accountCode()).isEqualTo(accumulated.getAccountId());
            assertThat(l.amount()).isEqualByComparingTo("1000.00");
        });

        BigDecimal expensesAfter = profitAndLossService.generateReport(APRIL_1, APRIL_30).totalExpenses();
        assertThat(expensesAfter.subtract(expensesBefore)).isEqualByComparingTo("1000.00");
    }

    @Test
    void theTrialBalanceAndTheRegisterReconcileToTheLedger() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        postRun(APRIL_30);

        List<TrialBalanceRowDto> tb = trialBalanceService.generateReport(APRIL_30).rows();
        TrialBalanceRowDto costRow = tb.stream().filter(r -> r.accountCode().equals(cost.getAccountId())).findFirst().orElseThrow();
        TrialBalanceRowDto depRow = tb.stream().filter(r -> r.accountCode().equals(accumulated.getAccountId())).findFirst().orElseThrow();
        assertThat(costRow.debit()).isEqualByComparingTo("12000.00");
        assertThat(depRow.credit()).isEqualByComparingTo("1000.00");

        ReconciliationReport rec = reports.reconciliation(APRIL_1, APRIL_30);
        assertThat(rec.rows()).filteredOn(r -> r.accountId().equals(cost.getId()) || r.accountId().equals(accumulated.getId()))
                .hasSize(2)
                .allSatisfy(r -> assertThat(r.difference()).isEqualByComparingTo("0"));
        ReconciliationRow depRec = rec.rows().stream().filter(r -> r.accountId().equals(accumulated.getId())).findFirst().orElseThrow();
        assertThat(depRec.subledgerBalance()).isEqualByComparingTo("-1000.00");

        assertThat(reports.balances(APRIL_30, FixedAssetReportService.ReportFilter.none()).rows())
                .filteredOn(r -> r.assetId().equals(asset.getId()))
                .singleElement().satisfies(r -> assertThat(r.carryingAmount()).isEqualByComparingTo("11000.00"));
        assertThat(reports.journalHistory(APRIL_1, APRIL_30, asset.getId()).rows()).hasSize(2);
    }

    @Test
    void theCashFlowAddsBackDepreciationOnceAndShowsOnlyTheCashPurchaseInInvesting() {
        CashFlowStatementResponse before = cashFlowService.generate(APRIL_1, APRIL_30);

        assets.activate(draft("12000", "0", 12).getId());
        postRun(APRIL_30);

        CashFlowStatementResponse after = cashFlowService.generate(APRIL_1, APRIL_30);
        assertThat(after.difference()).isEqualByComparingTo("0");
        assertThat(after.netProfit().subtract(before.netProfit())).isEqualByComparingTo("-1000.00");
        assertThat(line(after, "OPERATING", "DEPRECIATION").subtract(line(before, "OPERATING", "DEPRECIATION")))
                .isEqualByComparingTo("1000.00");
        assertThat(section(after, "OPERATING").subtract(section(before, "OPERATING"))).isEqualByComparingTo("0");
        assertThat(line(after, "INVESTING", "FIXED_ASSET_PURCHASES").subtract(line(before, "INVESTING", "FIXED_ASSET_PURCHASES")))
                .isEqualByComparingTo("-12000.00");
        assertThat(after.netCashFlow().subtract(before.netCashFlow())).isEqualByComparingTo("-12000.00");
    }

    @Test
    void anAssetBoughtOnCreditIsNotReportedAsAnInvestingCashOutflow() {
        CashFlowStatementResponse before = cashFlowService.generate(APRIL_1, APRIL_30);
        AccountEntity creditors = clone("2100", "92101", "Test Other Creditors");

        SaveFixedAssetRequest r = request("5000", "0", 12);
        r.setAcquisitionSource(AssetAcquisitionSource.OFFSET_ACCOUNT);
        r.setOffsetAccountId(creditors.getId());
        assets.activate(assets.create(r).getId());

        CashFlowStatementResponse after = cashFlowService.generate(APRIL_1, APRIL_30);
        assertThat(after.difference()).isEqualByComparingTo("0");
        assertThat(section(after, "INVESTING")).isEqualByComparingTo(section(before, "INVESTING"));
        assertThat(after.netCashFlow()).isEqualByComparingTo(before.netCashFlow());
    }

    // ------------------------------------------------------------------
    // Disposal and reversal
    // ------------------------------------------------------------------

    @Test
    void aSaleChargesDepreciationToTheDisposalDateRemovesTheAssetAndPostsTheGain() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        postRun(APRIL_30);
        postRun(MAY_31);

        DisposeAssetRequest sale = new DisposeAssetRequest();
        sale.setDisposalType(AssetDisposalType.SALE);
        sale.setDisposalDate(LocalDate.of(2026, 6, 15));
        sale.setProceeds(new BigDecimal("10000"));
        DisposalPreviewResponse preview = assets.previewDisposal(asset.getId(), sale);
        // June 1-15 is half a month of the 10 months left: 10,000 / 10 x 0.5
        assertThat(preview.getCatchUpDepreciation()).isEqualByComparingTo("500.00");
        assertThat(preview.getCarryingAmount()).isEqualByComparingTo("9500.00");
        assertThat(preview.getGainLoss()).isEqualByComparingTo("500.00");

        FixedAssetResponse sold = assets.dispose(asset.getId(), sale);

        assertThat(sold.getStatus()).isEqualTo(FixedAssetStatus.DISPOSED);
        assertThat(sold.getCostBalance()).isEqualByComparingTo("0");
        assertThat(sold.getCarryingAmount()).isEqualByComparingTo("0");
        JournalResponse journal = journalService.getById(sold.getDisposalJournalId());
        assertBalanced(journal);
        assertThat(creditOn(journal, cost)).isEqualByComparingTo("12000.00");
        assertThat(debitOn(journal, expense)).isEqualByComparingTo("500.00");
        assertThat(creditOn(journal, gain)).isEqualByComparingTo("500.00");

        List<TrialBalanceRowDto> tb = trialBalanceService.generateReport(JUNE_30).rows();
        assertThat(tb).noneMatch(row -> row.accountCode().equals(cost.getAccountId()) || row.accountCode().equals(accumulated.getAccountId()));
        DepreciationRunResponse june = runs.calculate(runRequest(JUNE_30));
        assertThat(june.getLines()).noneMatch(l -> l.getAssetId().equals(asset.getId()));
        assertThat(reports.disposals(APRIL_1, JUNE_30, FixedAssetReportService.ReportFilter.none()).rows())
                .filteredOn(row -> row.assetId().equals(asset.getId()))
                .singleElement().satisfies(row -> assertThat(row.gainLoss()).isEqualByComparingTo("500.00"));
    }

    @Test
    void aDisposalDatedBeforePostedDepreciationIsRefused() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        postRun(MAY_31);

        DisposeAssetRequest scrap = new DisposeAssetRequest();
        scrap.setDisposalType(AssetDisposalType.SCRAPPED);
        scrap.setDisposalDate(LocalDate.of(2026, 5, 10));

        assertThatThrownBy(() -> assets.dispose(asset.getId(), scrap))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Depreciation is posted through");
    }

    @Test
    void reversingARunKeepsTheHistoryRestoresTheAssetAndFreesThePeriod() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        DepreciationRunResponse april = postRun(APRIL_30);
        postRun(MAY_31);

        assertThatThrownBy(() -> runs.reverse(april.getId(), reason("Wrong period")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("has a later posting");

        DepreciationRunResponse may = runs.periods().stream().filter(p -> p.getEndDate().equals(MAY_31)).findFirst()
                .map(p -> runs.get(p.getRunId())).orElseThrow();
        DepreciationRunResponse reversed = runs.reverse(may.getId(), reason("Posted in error"));

        assertThat(reversed.getStatus()).isEqualTo(DepreciationRunStatus.REVERSED);
        assertThat(reversed.getReversalJournalId()).isNotNull();
        assertThat(journalService.getById(may.getJournalId()).getStatus()).isEqualTo(JournalStatus.REVERSED);
        assertBalanced(journalService.getById(reversed.getReversalJournalId()));
        FixedAssetResponse restored = assets.get(asset.getId());
        assertThat(restored.getAccumulatedDepreciation()).isEqualByComparingTo("1000.00");
        assertThat(restored.getDepreciatedThrough()).isEqualTo(APRIL_30);
        List<AssetTransactionResponse> history = assets.transactions(asset.getId());
        assertThat(history).extracting(AssetTransactionResponse::getType)
                .containsExactly(AssetTransactionType.ACQUISITION, AssetTransactionType.DEPRECIATION,
                        AssetTransactionType.DEPRECIATION, AssetTransactionType.REVERSAL);
        assertThat(history.get(2).getReversed()).isTrue();

        DepreciationRunResponse again = postRun(MAY_31);
        assertThat(lineFor(again, asset.getId()).getDepreciationAmount()).isEqualByComparingTo("1000.00");
        assertThat(assets.get(asset.getId()).getAccumulatedDepreciation()).isEqualByComparingTo("2000.00");
    }

    @Test
    void reversingTheAcquisitionReturnsTheAssetToDraft() {
        FixedAssetResponse asset = assets.activate(draft("12000", "0", 12).getId());
        Long acquisition = assets.transactions(asset.getId()).get(0).getId();

        AssetActionRequest request = new AssetActionRequest();
        request.setReason("Entered twice");
        FixedAssetResponse back = assets.reverseTransaction(asset.getId(), acquisition, request);

        assertThat(back.getStatus()).isEqualTo(FixedAssetStatus.DRAFT);
        assertThat(back.getCostBalance()).isEqualByComparingTo("0");
        assertThat(journalService.getById(asset.getAcquisitionJournalId()).getStatus()).isEqualTo(JournalStatus.REVERSED);
    }

    // ------------------------------------------------------------------
    // Schedule
    // ------------------------------------------------------------------

    @Test
    void theScheduleReconcilesPostedAndProjectedChargesToCostLessResidual() {
        FixedAssetResponse asset = assets.activate(draft("12000", "600", 12).getId());
        DepreciationRunResponse april = postRun(APRIL_30);

        List<ScheduleLineResponse> schedule = assets.schedule(asset.getId());

        assertThat(schedule.get(0).getStatus()).isEqualTo("POSTED");
        assertThat(schedule.get(0).getJournalNumber()).isEqualTo(april.getJournalNumber());
        assertThat(schedule.get(0).getDepreciationAmount()).isEqualByComparingTo("950.00");
        assertThat(schedule).filteredOn(r -> r.getStatus().equals("PROJECTED")).hasSize(11);
        BigDecimal total = schedule.stream().map(ScheduleLineResponse::getDepreciationAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("11400.00");
        assertThat(schedule.get(schedule.size() - 1).getClosingCarryingAmount()).isEqualByComparingTo("600.00");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private FixedAssetResponse draft(String amount, String residual, int life) {
        return assets.create(request(amount, residual, life));
    }

    private SaveFixedAssetRequest request(String amount, String residual, int life) {
        SaveFixedAssetRequest r = new SaveFixedAssetRequest();
        r.setName("Test asset " + amount);
        r.setCategoryId(categoryId);
        r.setAcquisitionDate(APRIL_1);
        r.setAvailableForUseDate(APRIL_1);
        r.setAcquisitionCost(new BigDecimal(amount));
        r.setResidualValue(new BigDecimal(residual));
        r.setUsefulLifeMonths(life);
        r.setDepreciationMethod(DepreciationMethod.STRAIGHT_LINE);
        r.setDepreciationConvention(DepreciationConvention.DAILY_PRORATA);
        r.setAcquisitionSource(AssetAcquisitionSource.BANK_PAYMENT);
        return r;
    }

    private DepreciationRunResponse postRun(LocalDate periodEnd) {
        DepreciationRunResponse run = runs.calculate(runRequest(periodEnd));
        run = runs.submit(run.getId());
        run = runs.approve(run.getId());
        return runs.post(run.getId());
    }

    private CreateDepreciationRunRequest runRequest(LocalDate periodEnd) {
        CreateDepreciationRunRequest r = new CreateDepreciationRunRequest();
        r.setAccountingPeriodId(periodRepository.findByDateInRange(periodEnd).orElseThrow().getId());
        return r;
    }

    private void setPeriodStatus(LocalDate date, AccountingPeriodStatus status) {
        AccountingPeriod p = periodRepository.findByDateInRange(date).orElseThrow();
        p.setStatus(status);
        periodRepository.saveAndFlush(p);
    }

    private static DepreciationRunActionRequest reason(String text) {
        DepreciationRunActionRequest r = new DepreciationRunActionRequest();
        r.setReason(text);
        return r;
    }

    private static DepreciationRunLineResponse lineFor(DepreciationRunResponse run, Long assetId) {
        return run.getLines().stream().filter(l -> l.getAssetId().equals(assetId)).findFirst().orElseThrow();
    }

    private AccountEntity clone(String templateCode, String code, String name) {
        AccountEntity template = accountRepository.findByAccountId(templateCode).orElseThrow();
        AccountEntity a = new AccountEntity();
        a.setAccountId(code);
        a.setAccountName(name);
        a.setCoaClearTo(template.getCoaClearTo());
        a.setIsActive(true);
        a.setIsControlAccount(false);
        a.setDeleted(false);
        AccountEntity saved = accountRepository.saveAndFlush(a);
        entityManager.flush();
        return saved;
    }

    private static void assertBalanced(JournalResponse j) {
        assertThat(j.getTotalDebit()).isEqualByComparingTo(j.getTotalCredit());
        BigDecimal debits = j.getLines().stream().map(JournalLineResponse::getDebitAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = j.getLines().stream().map(JournalLineResponse::getCreditAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debits).isEqualByComparingTo(credits);
    }

    private static BigDecimal debitOn(JournalResponse j, AccountEntity a) {
        return j.getLines().stream().filter(l -> a.getAccountId().equals(l.getAccountCode()))
                .map(JournalLineResponse::getDebitAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal creditOn(JournalResponse j, AccountEntity a) {
        return j.getLines().stream().filter(l -> a.getAccountId().equals(l.getAccountCode()))
                .map(JournalLineResponse::getCreditAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal balance(List<BalanceSheetAccountDto> rows, AccountEntity a) {
        return rows.stream().filter(r -> r.accountCode().equals(a.getAccountId())).map(BalanceSheetAccountDto::balance)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    private static BigDecimal section(CashFlowStatementResponse cf, String key) {
        return cf.sections().stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow().total();
    }

    private static BigDecimal line(CashFlowStatementResponse cf, String section, String key) {
        return cf.sections().stream().filter(s -> s.key().equals(section)).findFirst().orElseThrow()
                .lines().stream().filter(l -> l.key().equals(key)).map(CashFlowStatementResponse.Line::amount)
                .findFirst().orElse(BigDecimal.ZERO);
    }
}
