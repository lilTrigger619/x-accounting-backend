package com.unionsg.xaccounting.service.accounting;

import com.unionsg.xaccounting.MapperLayer.JournalMapper;
import com.unionsg.xaccounting.dto.accounting.YearEndClosingPreviewResponse;
import com.unionsg.xaccounting.dto.accounting.YearEndClosingResultResponse;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.reports.ProfitLossAccountDto;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportInternalDTO;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.FinancialPeriodAction;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import com.unionsg.xaccounting.enums.FinancialYearStatus;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.journal.JournalPostingService;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates Year-End Closing: previews the net profit/loss for a Financial Year,
 * posts a CLOSING journal that zeroes every temporary (INCOME/EXPENSE) account into
 * Retained Earnings, locks the year's periods, and supports a controlled reopen.
 */
@Service
@RequiredArgsConstructor
public class YearEndClosingService {

    private final FinancialYearService financialYearService;
    private final FinancialYearRepository financialYearRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final ProfitAndLossService profitAndLossService;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalPostingService journalPostingService;
    private final JournalMapper journalMapper;
    private final DocumentNumberService generalSequenceGeneratorService;
    private final FinancialPeriodAuditLogService auditLogService;

    private final com.unionsg.xaccounting.service.settings.AccountingMappingService accountingMappingService;

    @Transactional(readOnly = true)
    public YearEndClosingPreviewResponse getClosingPreview(Long financialYearId) {
        FinancialYear fy = financialYearService.getEntity(financialYearId);

        ProfitLossReportInternalDTO report =
                profitAndLossService.generateReport(fy.getStartDate(), fy.getEndDate());

        List<AccountingPeriod> periods =
                accountingPeriodRepository.findByFinancialYearIdOrderByPeriodNumberAsc(fy.getId());
        long open = periods.stream().filter(p -> p.getStatus() == AccountingPeriodStatus.OPEN).count();
        long locked = periods.stream().filter(p -> p.getStatus() == AccountingPeriodStatus.LOCKED).count();
        long closed = periods.stream().filter(p -> p.getStatus() == AccountingPeriodStatus.CLOSED).count();

        List<String> warnings = new ArrayList<>();
        if (fy.getStatus() == FinancialYearStatus.CLOSED) {
            warnings.add("This Financial Year is already closed.");
        }
        if (!fy.isHasOpeningBalance()) {
            warnings.add("No opening balance journal has been recorded for this Financial Year.");
        }
        if (open > 0) {
            warnings.add(open + " accounting period(s) are still open and will be automatically locked upon closing.");
        }

        BigDecimal netProfitLoss = report.totalRevenue().subtract(report.totalExpenses());

        return YearEndClosingPreviewResponse.builder()
                .financialYearId(fy.getId())
                .financialYearName(fy.getName())
                .startDate(fy.getStartDate())
                .endDate(fy.getEndDate())
                .totalRevenue(report.totalRevenue())
                .totalExpense(report.totalExpenses())
                .netProfitLoss(netProfitLoss)
                .revenueAccountCount(report.revenueAccounts().size())
                .expenseAccountCount(report.expenseAccounts().size())
                .totalPeriods(periods.size())
                .openPeriods((int) open)
                .lockedPeriods((int) locked)
                .closedPeriods((int) closed)
                .hasOpeningBalance(fy.isHasOpeningBalance())
                .alreadyClosed(fy.getStatus() == FinancialYearStatus.CLOSED)
                .warnings(warnings)
                .build();
    }

    @Transactional
    public YearEndClosingResultResponse closeFinancialYear(Long financialYearId) {
        FinancialYear fy = financialYearService.getEntity(financialYearId);

        if (fy.getStatus() == FinancialYearStatus.CLOSED) {
            throw new BadRequestException("Financial Year \"" + fy.getName() + "\" is already closed");
        }

        ProfitLossReportInternalDTO report =
                profitAndLossService.generateReport(fy.getStartDate(), fy.getEndDate());

        BigDecimal netProfitLoss = report.totalRevenue().subtract(report.totalExpenses());

        JournalResponse closingJournalResponse = postClosingJournal(fy, report, netProfitLoss);

        String previousStatus = fy.getStatus().name();
        fy.setStatus(FinancialYearStatus.CLOSED);
        fy.setClosedAt(LocalDateTime.now());
        fy.setClosedBy(SecurityUtils.getCurrentUser());
        fy.setTotalRevenue(report.totalRevenue());
        fy.setTotalExpense(report.totalExpenses());
        fy.setNetProfitLoss(netProfitLoss);
        fy.setRetainedEarningsMovement(netProfitLoss);
        if (closingJournalResponse != null) {
            fy.setClosingJournalId(closingJournalResponse.getId());
        }
        FinancialYear saved = financialYearRepository.save(fy);

        accountingPeriodService.lockAllForFinancialYear(fy.getId());

        auditLogService.record(FinancialPeriodEntityType.YEAR_END_CLOSING, saved.getId(),
                FinancialPeriodAction.CLOSED, previousStatus, saved.getStatus().name(),
                "Net " + (netProfitLoss.compareTo(BigDecimal.ZERO) >= 0 ? "profit" : "loss")
                        + " of " + netProfitLoss.abs() + " transferred to Retained Earnings");

        return YearEndClosingResultResponse.builder()
                .financialYear(financialYearService.toResponse(saved))
                .closingJournal(closingJournalResponse)
                .build();
    }

    @Transactional
    public YearEndClosingResultResponse reopenFinancialYear(Long financialYearId, String reason) {
        FinancialYear fy = financialYearService.getEntity(financialYearId);

        if (fy.getStatus() != FinancialYearStatus.CLOSED) {
            throw new BadRequestException("Only a closed Financial Year can be reopened");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A reason is required to reopen a closed Financial Year");
        }

        String previousStatus = fy.getStatus().name();
        fy.setStatus(FinancialYearStatus.OPEN);
        fy.setReopenedAt(LocalDateTime.now());
        fy.setReopenedBy(SecurityUtils.getCurrentUser());
        fy.setReopenReason(reason);
        FinancialYear saved = financialYearRepository.save(fy);

        auditLogService.record(FinancialPeriodEntityType.YEAR_END_CLOSING, saved.getId(),
                FinancialPeriodAction.REOPENED, previousStatus, saved.getStatus().name(), reason);

        JournalResponse closingJournal = saved.getClosingJournalId() != null
                ? journalMapper.toResponse(journalEntryRepository.findById(saved.getClosingJournalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Closing journal not found")))
                : null;

        return YearEndClosingResultResponse.builder()
                .financialYear(financialYearService.toResponse(saved))
                .closingJournal(closingJournal)
                .build();
    }

    private JournalResponse postClosingJournal(
            FinancialYear fy,
            ProfitLossReportInternalDTO report,
            BigDecimal netProfitLoss
    ) {
        List<JournalLine> lines = new ArrayList<>();
        int lineNumber = 1;

        for (ProfitLossAccountDto revenue : report.revenueAccounts()) {
            JournalLine line = zeroingLine(revenue, true, lineNumber);
            if (line != null) {
                lines.add(line);
                lineNumber++;
            }
        }

        for (ProfitLossAccountDto expense : report.expenseAccounts()) {
            JournalLine line = zeroingLine(expense, false, lineNumber);
            if (line != null) {
                lines.add(line);
                lineNumber++;
            }
        }

        if (lines.isEmpty() || netProfitLoss.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        String retainedEarningsAccountId = accountingMappingService.resolve(
                com.unionsg.xaccounting.enums.settings.MappingKey.CLOSING_RETAINED_EARNINGS);
        AccountEntity retainedEarnings = accountRepository.findByAccountId(retainedEarningsAccountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Retained Earnings account not found (accountId " + retainedEarningsAccountId + ")"));

        JournalLine retainedEarningsLine = new JournalLine();
        retainedEarningsLine.setAccount(retainedEarnings);
        retainedEarningsLine.setLineNumber(lineNumber);
        retainedEarningsLine.setDescription("Transfer of net "
                + (netProfitLoss.compareTo(BigDecimal.ZERO) >= 0 ? "profit" : "loss") + " to Retained Earnings");
        if (netProfitLoss.compareTo(BigDecimal.ZERO) > 0) {
            retainedEarningsLine.setDebitAmount(BigDecimal.ZERO);
            retainedEarningsLine.setCreditAmount(netProfitLoss);
        } else {
            retainedEarningsLine.setDebitAmount(netProfitLoss.abs());
            retainedEarningsLine.setCreditAmount(BigDecimal.ZERO);
        }
        lines.add(retainedEarningsLine);

        JournalEntry entry = new JournalEntry();
        entry.setJournalNumber(generalSequenceGeneratorService.generateNextNumber(DocumentModule.JOURNAL));
        entry.setJournalDate(fy.getEndDate());
        entry.setReference("CLOSE-" + fy.getName());
        entry.setDescription("Year-end closing journal for " + fy.getName());
        entry.setJournalType(JournalType.CLOSING);
        entry.setStatus(JournalStatus.DRAFT);
        entry.setSourceModule("YEAR_END_CLOSING");
        entry.setSourceEntityId(fy.getId());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            entry.addLine(line);
            totalDebit = totalDebit.add(line.getDebitAmount());
            totalCredit = totalCredit.add(line.getCreditAmount());
        }
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);

        journalPostingService.post(entry);

        JournalEntry saved = journalEntryRepository.save(entry);

        return journalMapper.toResponse(saved);
    }

    private JournalLine zeroingLine(ProfitLossAccountDto accountDto, boolean isRevenue, int lineNumber) {
        BigDecimal amount = accountDto.amount();
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        AccountEntity account = accountRepository.findById(accountDto.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountDto.accountName()));

        JournalLine line = new JournalLine();
        line.setAccount(account);
        line.setLineNumber(lineNumber);
        line.setDescription("Closing of " + accountDto.accountName());

        boolean normalPositive = amount.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal absAmount = amount.abs();

        // Revenue accounts normally carry a credit balance; zero it with a debit (and vice versa
        // for an unusual/contra balance). Expense accounts are the mirror image.
        boolean zeroWithDebit = isRevenue == normalPositive;

        if (zeroWithDebit) {
            line.setDebitAmount(absAmount);
            line.setCreditAmount(BigDecimal.ZERO);
        } else {
            line.setDebitAmount(BigDecimal.ZERO);
            line.setCreditAmount(absAmount);
        }

        return line;
    }
}
