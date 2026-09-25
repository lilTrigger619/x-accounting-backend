package com.unionsg.xaccounting.service.prepayment;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.prepayment.Prepayment;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentAmortizationLine;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Posts the GL impact of a {@link Prepayment} at each stage of its lifecycle (Prepayments
 * spec §9 "Prepayment Accounting"): the initial cash-out that creates the prepaid asset, each
 * period's recognition of that asset into expense, and a final write-off of whatever balance
 * is left unrecognized.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrepaymentJournalService {

    public static final String SOURCE_MODULE = "PREPAYMENT";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    /** Dr Prepaid Asset, Cr Bank/Cash - money paid out that has not yet been consumed. */
    @Transactional
    public JournalEntry postActivationJournal(Prepayment prepayment) {
        Long prepaidAccountId = resolveOverrideOrMapped(prepayment.getPrepaidAccount(), MappingKey.PREPAYMENT_DEFAULT_ASSET);
        Long bankAccountId = resolveBankAccountId(prepayment);

        List<CreateJournalLineRequest> lines = List.of(
                CreateJournalLineRequest.builder()
                        .accountId(prepaidAccountId)
                        .description("Prepayment made: " + prepayment.getPrepaymentNumber())
                        .debitAmount(prepayment.getTotalAmount())
                        .creditAmount(BigDecimal.ZERO)
                        .build(),
                CreateJournalLineRequest.builder()
                        .accountId(bankAccountId)
                        .description("Prepayment paid: " + prepayment.getPrepaymentNumber())
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(prepayment.getTotalAmount())
                        .build()
        );

        String description = "Prepayment " + prepayment.getPrepaymentNumber() + " paid to "
                + prepayment.getCounterpartyName() + ".";

        return createAndPostJournal(prepayment, prepayment.getPaymentDate(), lines, "", description);
    }

    /** Dr Expense, Cr Prepaid Asset - one period's worth of the asset consumed. */
    @Transactional
    public JournalEntry postRecognitionJournal(Prepayment prepayment, PrepaymentAmortizationLine line) {
        Long prepaidAccountId = resolveOverrideOrMapped(prepayment.getPrepaidAccount(), MappingKey.PREPAYMENT_DEFAULT_ASSET);
        Long expenseAccountId = resolveOverrideOrMapped(prepayment.getExpenseAccount(), MappingKey.PREPAYMENT_DEFAULT_EXPENSE);

        List<CreateJournalLineRequest> lines = List.of(
                CreateJournalLineRequest.builder()
                        .accountId(expenseAccountId)
                        .description("Prepayment recognized: " + prepayment.getPrepaymentNumber()
                                + " period " + line.getPeriodNumber())
                        .debitAmount(line.getAmount())
                        .creditAmount(BigDecimal.ZERO)
                        .build(),
                CreateJournalLineRequest.builder()
                        .accountId(prepaidAccountId)
                        .description("Prepaid asset consumed: " + prepayment.getPrepaymentNumber()
                                + " period " + line.getPeriodNumber())
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(line.getAmount())
                        .build()
        );

        String description = "Recognition of prepayment " + prepayment.getPrepaymentNumber()
                + " period " + line.getPeriodNumber() + ".";

        return createAndPostJournal(prepayment, line.getPeriodDate(),
                lines, "-P" + line.getPeriodNumber(), description);
    }

    /** Dr Expense, Cr Prepaid Asset - the whole remaining unrecognized balance in one shot. */
    @Transactional
    public JournalEntry postWriteOffJournal(Prepayment prepayment, BigDecimal amount, LocalDate date) {
        Long prepaidAccountId = resolveOverrideOrMapped(prepayment.getPrepaidAccount(), MappingKey.PREPAYMENT_DEFAULT_ASSET);
        Long expenseAccountId = resolveOverrideOrMapped(prepayment.getExpenseAccount(), MappingKey.PREPAYMENT_DEFAULT_EXPENSE);

        List<CreateJournalLineRequest> lines = List.of(
                CreateJournalLineRequest.builder()
                        .accountId(expenseAccountId)
                        .description("Prepayment written off: " + prepayment.getPrepaymentNumber())
                        .debitAmount(amount)
                        .creditAmount(BigDecimal.ZERO)
                        .build(),
                CreateJournalLineRequest.builder()
                        .accountId(prepaidAccountId)
                        .description("Prepaid asset written off: " + prepayment.getPrepaymentNumber())
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(amount)
                        .build()
        );

        String description = "Write-off of remaining balance for prepayment " + prepayment.getPrepaymentNumber() + ".";

        return createAndPostJournal(prepayment, date, lines, "-WO", description);
    }

    private JournalEntry createAndPostJournal(
            Prepayment prepayment, LocalDate date, List<CreateJournalLineRequest> lines,
            String referenceSuffix, String description
    ) {
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(date)
                .reference(prepayment.getPrepaymentNumber() + referenceSuffix)
                .description(description)
                .journalType(JournalType.PREPAYMENT)
                .currencyCode(prepayment.getCurrency() != null ? prepayment.getCurrency() : "USD")
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);

        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(prepayment.getId());
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());

        return journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting"));
    }

    private Long resolveOverrideOrMapped(AccountEntity override, MappingKey key) {
        if (override != null) {
            return resolveAccountId(override.getAccountId());
        }
        return resolveMappedAccountId(key);
    }

    private Long resolveBankAccountId(Prepayment prepayment) {
        if (prepayment.getBankAccount() != null) {
            return resolveAccountId(prepayment.getBankAccount().getGlAccountCode());
        }
        return resolveMappedAccountId(MappingKey.PREPAYMENT_BANK_ACCOUNT);
    }

    private Long resolveAccountId(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountId));
    }

    private Long resolveMappedAccountId(MappingKey key) {
        String code = accountingMappingService.resolve(key);
        return accountRepository.findByAccountId(code)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException(
                        "Required accounting configuration missing: \"" + key.getDescription() + "\" is mapped "
                                + "to account code \"" + code + "\", which does not exist in the Chart of Accounts. "
                                + "Configure it under Settings > Accounting Mappings."));
    }
}
