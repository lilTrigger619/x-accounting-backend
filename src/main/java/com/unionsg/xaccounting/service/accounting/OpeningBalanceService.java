package com.unionsg.xaccounting.service.accounting;

import com.unionsg.xaccounting.MapperLayer.JournalMapper;
import com.unionsg.xaccounting.dto.accounting.CreateOpeningBalanceRequest;
import com.unionsg.xaccounting.dto.accounting.OpeningBalanceLineRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.FinancialPeriodAction;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.journal.JournalPostingService;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Creates the one-time Opening Balance journal for a Financial Year, dated at the
 * year's start date. Account lines are resolved by their real primary key (the same
 * convention used by {@link YearEndClosingService} and {@link RecurringJournalService}),
 * independent of the numeric-account-code convention used by the manual journal entry
 * screen's {@code CreateJournalLineRequest}.
 */
@Service
@RequiredArgsConstructor
public class OpeningBalanceService {

    public static final String SOURCE_MODULE = "OPENING_BALANCE";

    private final FinancialYearRepository financialYearRepository;
    private final FinancialYearService financialYearService;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalPostingService journalPostingService;
    private final JournalMapper journalMapper;
    private final JournalService journalService;
    private final DocumentNumberService generalSequenceGeneratorService;
    private final FinancialPeriodAuditLogService auditLogService;

    @Transactional
    public JournalResponse create(CreateOpeningBalanceRequest request) {
        FinancialYear fy = financialYearService.getEntity(request.getFinancialYearId());

        if (fy.isHasOpeningBalance()) {
            throw new BadRequestException(
                    "An opening balance journal already exists for \"" + fy.getName() + "\"");
        }

        JournalEntry entry = new JournalEntry();
        entry.setJournalNumber(generalSequenceGeneratorService.generateNextNumber(DocumentModule.JOURNAL));
        entry.setJournalDate(fy.getStartDate());
        entry.setReference("OB-" + fy.getName());
        entry.setDescription("Opening balances for " + fy.getName());
        entry.setJournalType(JournalType.OPENING_BALANCE);
        entry.setStatus(JournalStatus.DRAFT);
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(fy.getId());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int lineNumber = 1;

        for (OpeningBalanceLineRequest lineRequest : request.getLines()) {
            BigDecimal debit = defaultAmount(lineRequest.getDebitAmount());
            BigDecimal credit = defaultAmount(lineRequest.getCreditAmount());
            boolean hasDebit = debit.compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = credit.compareTo(BigDecimal.ZERO) > 0;
            if (hasDebit == hasCredit) {
                throw new BadRequestException("Each line must contain either a debit or a credit amount");
            }

            AccountEntity account = accountRepository.findById(lineRequest.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            JournalLine line = new JournalLine();
            line.setAccount(account);
            line.setLineNumber(lineNumber++);
            line.setDescription(lineRequest.getDescription());
            line.setDebitAmount(debit);
            line.setCreditAmount(credit);
            entry.addLine(line);

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BadRequestException("Opening balance lines must balance (total debit must equal total credit)");
        }
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);

        journalPostingService.post(entry);

        JournalEntry saved = journalEntryRepository.save(entry);

        fy.setHasOpeningBalance(true);
        financialYearRepository.save(fy);

        auditLogService.record(FinancialPeriodEntityType.OPENING_BALANCE, fy.getId(),
                FinancialPeriodAction.CREATED, null, "POSTED",
                "Opening balance journal " + saved.getJournalNumber() + " posted");

        return journalMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JournalResponse getForFinancialYear(Long financialYearId) {
        JournalEntry entry = journalEntryRepository
                .findBySourceModuleAndSourceEntityIdAndStatus(SOURCE_MODULE, financialYearId, JournalStatus.POSTED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No opening balance journal found for this Financial Year"));
        return journalService.getById(entry.getId());
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
