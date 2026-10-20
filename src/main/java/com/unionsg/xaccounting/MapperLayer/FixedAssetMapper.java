package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.fixedasset.AccountRef;
import com.unionsg.xaccounting.dto.fixedasset.AssetEstimateChangeResponse;
import com.unionsg.xaccounting.dto.fixedasset.AssetTransactionResponse;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunLineResponse;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunListItemResponse;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetAuditLogResponse;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetListItemResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.fixedasset.AssetEstimateChange;
import com.unionsg.xaccounting.entity.fixedasset.AssetTransaction;
import com.unionsg.xaccounting.entity.fixedasset.DepreciationRun;
import com.unionsg.xaccounting.entity.fixedasset.DepreciationRunLine;
import com.unionsg.xaccounting.entity.fixedasset.FixedAsset;
import com.unionsg.xaccounting.entity.fixedasset.FixedAssetAuditLog;

public class FixedAssetMapper {

    private FixedAssetMapper() {
        throw new UnsupportedOperationException("Mapper class cannot be instantiated");
    }

    public static AccountRef toRef(AccountEntity a) {
        if (a == null) return null;
        AccountRef r = new AccountRef();
        r.setId(a.getId());
        r.setCode(a.getAccountId());
        r.setName(a.getAccountName());
        return r;
    }

    public static String userName(User u) {
        return u != null ? u.getFullName() : null;
    }

    public static FixedAssetListItemResponse toListItem(FixedAsset a) {
        FixedAssetListItemResponse r = new FixedAssetListItemResponse();
        r.setId(a.getId());
        r.setAssetNumber(a.getAssetNumber());
        r.setName(a.getName());
        if (a.getCategory() != null) {
            r.setCategoryId(a.getCategory().getId());
            r.setCategoryName(a.getCategory().getName());
        }
        r.setLocationName(a.getLocation() != null ? a.getLocation().getName() : null);
        r.setDepartmentName(a.getDepartment() != null ? a.getDepartment().getName() : null);
        r.setStatus(a.getStatus());
        r.setDepreciationMethod(a.getDepreciationMethod());
        r.setAcquisitionDate(a.getAcquisitionDate());
        r.setAvailableForUseDate(a.getAvailableForUseDate());
        r.setAcquisitionCost(a.getAcquisitionCost());
        r.setCostBalance(a.getCostBalance());
        r.setAccumulatedDepreciation(a.getAccumulatedDepreciation());
        r.setAccumulatedImpairment(a.getAccumulatedImpairment());
        r.setCarryingAmount(a.getCarryingAmount());
        r.setDepreciatedThrough(a.getDepreciatedThrough());
        return r;
    }

    public static AssetTransactionResponse toTransaction(AssetTransaction t, boolean reversible) {
        AssetTransactionResponse r = new AssetTransactionResponse();
        r.setId(t.getId());
        r.setType(t.getType());
        r.setMovementType(t.getMovementType());
        r.setTransactionDate(t.getTransactionDate());
        r.setCostAmount(t.getCostAmount());
        r.setDepreciationAmount(t.getDepreciationAmount());
        r.setImpairmentAmount(t.getImpairmentAmount());
        r.setProceedsAmount(t.getProceedsAmount());
        r.setGainLossAmount(t.getGainLossAmount());
        if (t.getJournal() != null) {
            r.setJournalId(t.getJournal().getId());
            r.setJournalNumber(t.getJournal().getJournalNumber());
        }
        if (t.getRunLine() != null) {
            r.setRunId(t.getRunLine().getRun().getId());
            r.setRunNumber(t.getRunLine().getRun().getRunNumber());
        }
        r.setReversesId(t.getReverses() != null ? t.getReverses().getId() : null);
        r.setReversed(t.isReversed());
        r.setReversible(reversible);
        r.setDescription(t.getDescription());
        r.setCreatedBy(userName(t.getCreatedBy()));
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }

    public static DepreciationRunLineResponse toLine(DepreciationRunLine l) {
        DepreciationRunLineResponse r = new DepreciationRunLineResponse();
        r.setId(l.getId());
        FixedAsset a = l.getAsset();
        r.setAssetId(a.getId());
        r.setAssetNumber(a.getAssetNumber());
        r.setAssetName(a.getName());
        r.setCategoryName(a.getCategory() != null ? a.getCategory().getName() : null);
        r.setStatus(l.getStatus());
        r.setFromDate(l.getFromDate());
        r.setToDate(l.getToDate());
        r.setMethod(l.getMethod());
        r.setConvention(l.getConvention());
        r.setOpeningCost(l.getOpeningCost());
        r.setOpeningAccumulatedDepreciation(l.getOpeningAccumulatedDepreciation());
        r.setOpeningAccumulatedImpairment(l.getOpeningAccumulatedImpairment());
        r.setOpeningCarryingAmount(l.getOpeningCarryingAmount());
        r.setResidualValue(l.getResidualValue());
        r.setUsefulLifeMonths(l.getUsefulLifeMonths());
        r.setRemainingLifeMonths(l.getRemainingLifeMonths());
        r.setMonthsCharged(l.getMonthsCharged());
        r.setAnnualRate(l.getAnnualRate());
        r.setDepreciationAmount(l.getDepreciationAmount());
        r.setClosingAccumulatedDepreciation(l.getClosingAccumulatedDepreciation());
        r.setClosingCarryingAmount(l.getClosingCarryingAmount());
        r.setExpenseAccount(toRef(l.getExpenseAccount()));
        r.setAccumulatedDepreciationAccount(toRef(l.getAccumulatedDepreciationAccount()));
        r.setExplanation(l.getExplanation());
        r.setMessage(l.getMessage());
        return r;
    }

    public static DepreciationRunListItemResponse toRunListItem(DepreciationRun run) {
        DepreciationRunListItemResponse r = new DepreciationRunListItemResponse();
        r.setId(run.getId());
        r.setRunNumber(run.getRunNumber());
        r.setPeriodName(run.getAccountingPeriod() != null ? run.getAccountingPeriod().getName() : null);
        r.setPeriodStart(run.getPeriodStart());
        r.setPeriodEnd(run.getPeriodEnd());
        r.setPostingDate(run.getPostingDate());
        r.setStatus(run.getStatus());
        r.setTotalDepreciation(run.getTotalDepreciation());
        r.setAssetCount(run.getAssetCount());
        r.setErrorCount(run.getErrorCount());
        if (run.getJournal() != null) {
            r.setJournalId(run.getJournal().getId());
            r.setJournalNumber(run.getJournal().getJournalNumber());
        }
        if (run.getReversalJournal() != null) {
            r.setReversalJournalId(run.getReversalJournal().getId());
            r.setReversalJournalNumber(run.getReversalJournal().getJournalNumber());
        }
        r.setPostedBy(userName(run.getPostedBy()));
        r.setPostedAt(run.getPostedAt());
        r.setCreatedAt(run.getCreatedAt());
        return r;
    }

    public static FixedAssetAuditLogResponse toAudit(FixedAssetAuditLog log) {
        FixedAssetAuditLogResponse r = new FixedAssetAuditLogResponse();
        r.setId(log.getId());
        r.setAssetId(log.getAssetId());
        r.setRunId(log.getRunId());
        r.setAction(log.getAction());
        r.setPreviousStatus(log.getPreviousStatus());
        r.setNewStatus(log.getNewStatus());
        r.setDetails(log.getDetails());
        r.setPerformedBy(userName(log.getPerformedBy()));
        r.setPerformedAt(log.getPerformedAt());
        return r;
    }

    public static AssetEstimateChangeResponse toEstimateChange(AssetEstimateChange c) {
        AssetEstimateChangeResponse r = new AssetEstimateChangeResponse();
        r.setId(c.getId());
        r.setEffectiveDate(c.getEffectiveDate());
        r.setPreviousUsefulLifeMonths(c.getPreviousUsefulLifeMonths());
        r.setNewUsefulLifeMonths(c.getNewUsefulLifeMonths());
        r.setPreviousResidualValue(c.getPreviousResidualValue());
        r.setNewResidualValue(c.getNewResidualValue());
        r.setPreviousMethod(c.getPreviousMethod());
        r.setNewMethod(c.getNewMethod());
        r.setPreviousConvention(c.getPreviousConvention());
        r.setNewConvention(c.getNewConvention());
        r.setPreviousAnnualRate(c.getPreviousAnnualRate());
        r.setNewAnnualRate(c.getNewAnnualRate());
        r.setReason(c.getReason());
        r.setCreatedBy(userName(c.getCreatedBy()));
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
}
