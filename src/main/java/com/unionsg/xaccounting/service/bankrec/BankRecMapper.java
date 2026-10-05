package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.bankrec.AdjustmentResponse;
import com.unionsg.xaccounting.dto.bankrec.AuditLogResponse;
import com.unionsg.xaccounting.dto.bankrec.ImportProfileResponse;
import com.unionsg.xaccounting.dto.bankrec.MatchItemResponse;
import com.unionsg.xaccounting.dto.bankrec.MatchResponse;
import com.unionsg.xaccounting.dto.bankrec.MatchingRuleResponse;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationResponse;
import com.unionsg.xaccounting.dto.bankrec.StatementImportResponse;
import com.unionsg.xaccounting.dto.bankrec.StatementTransactionResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.bankrec.BankMatchingRule;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliation;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAdjustment;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAuditLog;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatch;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatchItem;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImport;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImportProfile;
import com.unionsg.xaccounting.entity.bankrec.BankStatementTransaction;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.bankrec.MatchItemSide;
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;

import java.math.BigDecimal;

/** Entity to response mapping for the bank reconciliation module. */
public final class BankRecMapper {

    private BankRecMapper() {
    }

    public static ReconciliationResponse toResponse(BankReconciliation r) {
        BankAccount account = r.getBankAccount();
        return ReconciliationResponse.builder()
                .id(r.getId())
                .reconciliationNumber(r.getReconciliationNumber())
                .bankAccountId(account.getId())
                .bankAccountName(account.getAccountName())
                .bankName(account.getBankName())
                .bankAccountNumber(account.getAccountNumber())
                .glAccountCode(account.getGlAccountCode())
                .currency(r.getCurrency())
                .statementDate(r.getStatementDate())
                .periodStart(r.getPeriodStart())
                .periodEnd(r.getPeriodEnd())
                .openingBankBalance(r.getOpeningBankBalance())
                .closingBankBalance(r.getClosingBankBalance())
                .bookBalance(r.getBookBalance())
                .outstandingDeposits(r.getOutstandingDeposits())
                .outstandingWithdrawals(r.getOutstandingWithdrawals())
                .bankCharges(r.getBankCharges())
                .bankInterest(r.getBankInterest())
                .adjustmentsTotal(r.getAdjustmentsTotal())
                .difference(r.getDifference())
                .tolerance(r.getTolerance())
                .withinTolerance(r.getDifference().abs().compareTo(r.getTolerance().abs()) <= 0)
                .status(r.getStatus())
                .notes(r.getNotes())
                .preparedByName(r.getPreparedByName())
                .preparedAt(r.getPreparedAt())
                .reviewedByName(r.getReviewedByName())
                .reviewedAt(r.getReviewedAt())
                .reviewNotes(r.getReviewNotes())
                .completedByName(r.getCompletedByName())
                .completedAt(r.getCompletedAt())
                .differenceOverridden(r.getDifferenceOverridden())
                .overrideReason(r.getOverrideReason())
                .reopenedByName(r.getReopenedByName())
                .reopenedAt(r.getReopenedAt())
                .reopenReason(r.getReopenReason())
                .reopenCount(r.getReopenCount())
                .cancelledByName(r.getCancelledByName())
                .cancelledAt(r.getCancelledAt())
                .cancelReason(r.getCancelReason())
                .lastAutoMatchAt(r.getLastAutoMatchAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    /** {@code matchedAsOf} is the amount matched as at the reconciliation being viewed. */
    public static StatementTransactionResponse toResponse(BankStatementTransaction t, BigDecimal matchedAsOf) {
        BigDecimal matched = matchedAsOf != null ? matchedAsOf : t.getMatchedAmount();
        BigDecimal remaining = t.getAbsoluteAmount().subtract(matched);
        return StatementTransactionResponse.builder()
                .id(t.getId())
                .bankAccountId(t.getBankAccount().getId())
                .statementImportId(t.getStatementImport().getId())
                .sourceRowNumber(t.getSourceRowNumber())
                .transactionDate(t.getTransactionDate())
                .valueDate(t.getValueDate())
                .description(t.getDescription())
                .reference(t.getReference())
                .debitAmount(t.getDebitAmount())
                .creditAmount(t.getCreditAmount())
                .amount(t.getAmount())
                .runningBalance(t.getRunningBalance())
                .externalTransactionId(t.getExternalTransactionId())
                .matchedAmount(matched)
                .remainingAmount(remaining)
                .status(statementStatus(t.getAbsoluteAmount(), matched))
                .clearedInReconciliationId(t.getClearedInReconciliation() != null ? t.getClearedInReconciliation().getId() : null)
                .importedAt(t.getCreatedAt())
                .build();
    }

    public static StatementTransactionStatus statementStatus(BigDecimal absoluteAmount, BigDecimal matched) {
        if (matched == null || matched.signum() <= 0) {
            return StatementTransactionStatus.UNMATCHED;
        }
        return matched.compareTo(absoluteAmount) >= 0 ? StatementTransactionStatus.MATCHED : StatementTransactionStatus.PARTIALLY_MATCHED;
    }

    public static StatementImportResponse toResponse(BankStatementImport i) {
        return StatementImportResponse.builder()
                .id(i.getId())
                .bankAccountId(i.getBankAccount().getId())
                .bankAccountName(i.getBankAccount().getAccountName())
                .reconciliationId(i.getReconciliation() != null ? i.getReconciliation().getId() : null)
                .reconciliationNumber(i.getReconciliation() != null ? i.getReconciliation().getReconciliationNumber() : null)
                .profileId(i.getProfile() != null ? i.getProfile().getId() : null)
                .profileName(i.getProfile() != null ? i.getProfile().getName() : null)
                .fileName(i.getFileName())
                .firstTransactionDate(i.getFirstTransactionDate())
                .lastTransactionDate(i.getLastTransactionDate())
                .totalRows(i.getTotalRows())
                .importedRows(i.getImportedRows())
                .duplicateRows(i.getDuplicateRows())
                .errorRows(i.getErrorRows())
                .importedByName(i.getImportedByName())
                .importedAt(i.getCreatedAt())
                .dryRun(false)
                .build();
    }

    public static MatchResponse toResponse(BankReconciliationMatch m) {
        return MatchResponse.builder()
                .id(m.getId())
                .reconciliationId(m.getReconciliation().getId())
                .matchType(m.getMatchType())
                .status(m.getStatus())
                .confidence(m.getConfidence())
                .ruleName(m.getRuleName())
                .matchReasons(m.getMatchReasons())
                .matchedAmount(m.getMatchedAmount())
                .notes(m.getNotes())
                .matchedByName(m.getMatchedByName())
                .matchedAt(m.getMatchedAt())
                .confirmedByName(m.getConfirmedByName())
                .confirmedAt(m.getConfirmedAt())
                .unmatchedByName(m.getUnmatchedByName())
                .unmatchedAt(m.getUnmatchedAt())
                .unmatchReason(m.getUnmatchReason())
                .items(m.getItems().stream().map(BankRecMapper::toResponse).toList())
                .build();
    }

    public static MatchItemResponse toResponse(BankReconciliationMatchItem i) {
        MatchItemResponse.MatchItemResponseBuilder b = MatchItemResponse.builder()
                .id(i.getId())
                .side(i.getSide())
                .allocatedAmount(i.getAmount());
        if (i.getSide() == MatchItemSide.STATEMENT && i.getStatementTransaction() != null) {
            BankStatementTransaction t = i.getStatementTransaction();
            b.statementTransactionId(t.getId())
                    .date(t.getTransactionDate())
                    .description(t.getDescription())
                    .reference(t.getReference())
                    .itemAmount(t.getAmount());
        } else if (i.getJournalLine() != null) {
            JournalLine line = i.getJournalLine();
            JournalEntry je = line.getJournalEntry();
            b.journalLineId(line.getId())
                    .journalNumber(je.getJournalNumber())
                    .date(je.getJournalDate())
                    .description(line.getDescription() != null ? line.getDescription() : je.getDescription())
                    .reference(je.getReference())
                    .itemAmount(line.getDebitAmount().subtract(line.getCreditAmount()));
        }
        return b.build();
    }

    public static AdjustmentResponse toResponse(BankReconciliationAdjustment a) {
        return AdjustmentResponse.builder()
                .id(a.getId())
                .reconciliationId(a.getReconciliation().getId())
                .adjustmentType(a.getAdjustmentType())
                .adjustmentTypeLabel(a.getAdjustmentType().getLabel())
                .moneyIn(a.getAdjustmentType().isMoneyIn())
                .transactionDate(a.getTransactionDate())
                .amount(a.getAmount())
                .signedAmount(a.getSignedAmount())
                .description(a.getDescription())
                .reference(a.getReference())
                .offsetAccountId(a.getOffsetAccount().getId())
                .offsetAccountCode(a.getOffsetAccount().getAccountId())
                .offsetAccountName(a.getOffsetAccount().getAccountName())
                .statementTransactionId(a.getStatementTransaction() != null ? a.getStatementTransaction().getId() : null)
                .journalId(a.getJournal() != null ? a.getJournal().getId() : null)
                .journalNumber(a.getJournal() != null ? a.getJournal().getJournalNumber() : null)
                .reversalJournalId(a.getReversalJournalId())
                .status(a.getStatus())
                .postedByName(a.getPostedByName())
                .postedAt(a.getPostedAt())
                .reversedByName(a.getReversedByName())
                .reversedAt(a.getReversedAt())
                .reversalReason(a.getReversalReason())
                .build();
    }

    public static AuditLogResponse toResponse(BankReconciliationAuditLog l) {
        return AuditLogResponse.builder()
                .id(l.getId())
                .reconciliationId(l.getReconciliationId())
                .bankAccountId(l.getBankAccountId())
                .action(l.getAction())
                .details(l.getDetails())
                .userName(l.getUserName())
                .occurredAt(l.getOccurredAt())
                .build();
    }

    public static MatchingRuleResponse toResponse(BankMatchingRule r) {
        return MatchingRuleResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .priority(r.getPriority())
                .active(r.getActive())
                .bankAccountId(r.getBankAccount() != null ? r.getBankAccount().getId() : null)
                .bankAccountName(r.getBankAccount() != null ? r.getBankAccount().getAccountName() : null)
                .dateToleranceDays(r.getDateToleranceDays())
                .matchReference(r.getMatchReference())
                .matchTransactionNumber(r.getMatchTransactionNumber())
                .matchDescription(r.getMatchDescription())
                .matchChequeNumber(r.getMatchChequeNumber())
                .matchCounterparty(r.getMatchCounterparty())
                .autoConfirmThreshold(r.getAutoConfirmThreshold())
                .suggestThreshold(r.getSuggestThreshold())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public static ImportProfileResponse toResponse(BankStatementImportProfile p) {
        return ImportProfileResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .bankAccountId(p.getBankAccount() != null ? p.getBankAccount().getId() : null)
                .bankAccountName(p.getBankAccount() != null ? p.getBankAccount().getAccountName() : null)
                .delimiter(p.getDelimiter())
                .hasHeaderRow(p.getHasHeaderRow())
                .skipRows(p.getSkipRows())
                .dateFormat(p.getDateFormat())
                .transactionDateColumn(p.getTransactionDateColumn())
                .valueDateColumn(p.getValueDateColumn())
                .descriptionColumn(p.getDescriptionColumn())
                .referenceColumn(p.getReferenceColumn())
                .debitColumn(p.getDebitColumn())
                .creditColumn(p.getCreditColumn())
                .amountColumn(p.getAmountColumn())
                .balanceColumn(p.getBalanceColumn())
                .externalIdColumn(p.getExternalIdColumn())
                .amountSignConvention(p.getAmountSignConvention())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
