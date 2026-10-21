package com.unionsg.xaccounting.dto.fixedasset.report;

import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunListItemResponse;
import com.unionsg.xaccounting.dto.fixedasset.PeriodDepreciationStatusResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Response shapes of the fixed asset reports. Amounts are positive in the way a reader expects. */
public final class FixedAssetReports {

    private FixedAssetReports() {
    }

    /** One asset's balances on a date. */
    public record AssetBalanceRow(Long assetId, String assetNumber, String assetName, Long categoryId, String categoryName,
                                  String locationName, String departmentName, LocalDate acquisitionDate,
                                  LocalDate availableForUseDate, String depreciationMethod, String status,
                                  BigDecimal cost, BigDecimal accumulatedDepreciation, BigDecimal accumulatedImpairment,
                                  BigDecimal carryingAmount, BigDecimal residualValue, Integer usefulLifeMonths) {
    }

    public record CategoryTotal(Long categoryId, String categoryName, long assetCount, BigDecimal cost,
                                BigDecimal accumulatedDepreciation, BigDecimal accumulatedImpairment, BigDecimal carryingAmount) {
    }

    /** The register and the net book value report: balances per asset on a date, with category totals. */
    public record BalancesReport(LocalDate asOfDate, List<AssetBalanceRow> rows, List<CategoryTotal> byCategory,
                                 CategoryTotal total) {
    }

    /** One posted depreciation charge or its reversal. */
    public record DepreciationMovementRow(Long transactionId, Long assetId, String assetNumber, String assetName,
                                          String categoryName, LocalDate date, String type, BigDecimal amount,
                                          Long journalId, String journalNumber, Long runId, String runNumber,
                                          String periodName, String description) {
    }

    public record PeriodTotal(String period, LocalDate startDate, LocalDate endDate, BigDecimal amount,
                              Map<String, BigDecimal> byCategory) {
    }

    public record DepreciationByPeriodReport(LocalDate fromDate, LocalDate toDate, List<PeriodTotal> periods,
                                             List<DepreciationMovementRow> rows, BigDecimal total) {
    }

    /** Opening + charge − reversals − disposals = closing, per asset. */
    public record RollForwardRow(Long assetId, String assetNumber, String assetName, String categoryName,
                                 BigDecimal opening, BigDecimal charge, BigDecimal reversals, BigDecimal impairment,
                                 BigDecimal disposals, BigDecimal closing) {
    }

    public record AccumulatedDepreciationReport(LocalDate fromDate, LocalDate toDate, List<RollForwardRow> rows,
                                                List<RollForwardRow> byCategory, RollForwardRow total) {
    }

    public record AcquisitionRow(Long transactionId, Long assetId, String assetNumber, String assetName, String categoryName,
                                 LocalDate acquisitionDate, BigDecimal cost, BigDecimal depreciationBroughtForward,
                                 String acquisitionSource, Long journalId, String journalNumber, boolean reversed) {
    }

    public record AcquisitionReport(LocalDate fromDate, LocalDate toDate, List<AcquisitionRow> rows, BigDecimal totalCost) {
    }

    public record DisposalRow(Long transactionId, Long assetId, String assetNumber, String assetName, String categoryName,
                              LocalDate disposalDate, String disposalType, BigDecimal cost,
                              BigDecimal accumulatedDepreciation, BigDecimal accumulatedImpairment,
                              BigDecimal carryingAmount, BigDecimal proceeds, BigDecimal gainLoss,
                              Long journalId, String journalNumber, boolean reversed) {
    }

    public record DisposalReport(LocalDate fromDate, LocalDate toDate, List<DisposalRow> rows, BigDecimal totalCost,
                                 BigDecimal totalCarryingAmount, BigDecimal totalProceeds, BigDecimal totalGainLoss) {
    }

    /**
     * One GL account the register posts to. Balances are debit-positive, so accumulated depreciation
     * and impairment are negative.
     */
    public record ReconciliationRow(Long accountId, String accountCode, String accountName, String roles,
                                    BigDecimal subledgerBalance, BigDecimal ledgerBalance, BigDecimal difference) {
    }

    public record OutsideJournalRow(Long journalId, String journalNumber, LocalDate journalDate, String description,
                                    String sourceModule, Long accountId, String accountCode, String accountName,
                                    BigDecimal debit, BigDecimal credit) {
    }

    public record ReconciliationReport(LocalDate fromDate, LocalDate asOfDate, List<ReconciliationRow> rows,
                                       BigDecimal subledgerCarryingAmount, BigDecimal ledgerCarryingAmount,
                                       BigDecimal difference, boolean reconciled, List<OutsideJournalRow> outsideJournals) {
    }

    public record JournalHistoryRow(Long journalId, String journalNumber, LocalDate journalDate, String journalType,
                                    String status, String description, BigDecimal amount, String sourceModule,
                                    Long sourceId, String sourceNumber, Long reversalOfJournalId) {
    }

    public record JournalHistoryReport(LocalDate fromDate, LocalDate toDate, List<JournalHistoryRow> rows) {
    }

    public record DashboardResponse(LocalDate asOfDate, Map<String, Long> assetsByStatus, CategoryTotal totals,
                                    List<CategoryTotal> byCategory, BigDecimal depreciationThisPeriod,
                                    BigDecimal depreciationYearToDate, PeriodDepreciationStatusResponse currentPeriod,
                                    List<DepreciationRunListItemResponse> recentRuns, long runsAwaitingAction,
                                    BigDecimal reconciliationDifference, long assetsWithAccountProblems) {
    }
}
