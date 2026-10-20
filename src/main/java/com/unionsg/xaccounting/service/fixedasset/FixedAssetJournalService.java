package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.ReverseJournalRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.banking.BaseCurrencyService;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Posts the fixed asset module's journals through the normal journal create/post path, so the
 * period lock, numbering and the balance check all apply exactly as they do to every other module.
 *
 * <pre>
 * acquisition   Dr Asset Cost                    Cr Bank / offset account (Cr Accumulated Depreciation for depreciation brought forward)
 * depreciation  Dr Depreciation Expense          Cr Accumulated Depreciation          (one pair per asset, journal type DEPRECIATION)
 * impairment    Dr Impairment Loss               Cr Accumulated Impairment
 * disposal      Dr Accumulated Depreciation, Dr Accumulated Impairment, Dr Proceeds, Dr Loss on Disposal
 *                                                Cr Asset Cost, Cr Gain on Disposal
 * </pre>
 *
 * A posted journal is never edited; mistakes are reversed with a mirror-image journal.
 */
@Service
@RequiredArgsConstructor
public class FixedAssetJournalService {

    public static final String SOURCE_ASSET = "FIXED_ASSET";
    public static final String SOURCE_RUN = "DEPRECIATION_RUN";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final PeriodLockGuard periodLockGuard;
    private final BaseCurrencyService baseCurrencyService;

    /** Builds a line; exactly one of debit and credit is non-zero. */
    public static CreateJournalLineRequest debit(AccountEntity account, String description, BigDecimal amount) {
        return line(account, description, amount, BigDecimal.ZERO);
    }

    public static CreateJournalLineRequest credit(AccountEntity account, String description, BigDecimal amount) {
        return line(account, description, BigDecimal.ZERO, amount);
    }

    @Transactional
    public JournalEntry post(JournalType type, LocalDate date, String reference, String description,
                             List<CreateJournalLineRequest> lines, String sourceModule, Long sourceId) {
        periodLockGuard.assertPostable(date);
        List<CreateJournalLineRequest> nonZero = new ArrayList<>();
        for (CreateJournalLineRequest l : lines) {
            if (nz(l.getDebitAmount()).signum() != 0 || nz(l.getCreditAmount()).signum() != 0) nonZero.add(l);
        }
        BigDecimal debits = nonZero.stream().map(l -> nz(l.getDebitAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = nonZero.stream().map(l -> nz(l.getCreditAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debits.compareTo(credits) != 0) {
            throw new BusinessException("The " + description + " journal does not balance (debits " + debits
                    + ", credits " + credits + "). Nothing was posted.");
        }
        if (nonZero.size() < 2) {
            throw new BusinessException("There is nothing to post for " + description + ".");
        }
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(date)
                .reference(reference)
                .description(description)
                .journalType(type)
                .currencyCode(baseCurrencyService.resolve())
                .lines(nonZero)
                .build();
        JournalResponse created = journalService.create(request);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule(sourceModule);
        entry.setSourceEntityId(sourceId);
        entry.setSystemGenerated(true);
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());
        JournalEntry result = journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting"));
        if (result.getTotalDebit().compareTo(result.getTotalCredit()) != 0) {
            throw new BusinessException("Posted journal " + result.getJournalNumber() + " does not balance");
        }
        return result;
    }

    /** Posts the mirror image of a module journal on {@code date}; the original is marked REVERSED, never edited. */
    @Transactional
    public JournalEntry reverse(JournalEntry journal, String reason, LocalDate date) {
        JournalResponse reversal = journalService.reverse(journal.getId(),
                ReverseJournalRequest.builder().reason(reason).reverseDate(date).build());
        JournalEntry entry = journalEntryRepository.findById(reversal.getId())
                .orElseThrow(() -> new BusinessException("Reversal journal not found after posting"));
        entry.setSourceModule(journal.getSourceModule());
        entry.setSourceEntityId(journal.getSourceEntityId());
        entry.setSystemGenerated(true);
        return journalEntryRepository.save(entry);
    }

    /** The reversal date to use when the caller names none: the original date if still open, else today. */
    public LocalDate defaultReversalDate(LocalDate original, LocalDate requested) {
        if (requested != null) return requested;
        if (original != null && periodLockGuard.isPostable(original)) return original;
        LocalDate today = LocalDate.now();
        return original != null && original.isAfter(today) ? original : today;
    }

    private static CreateJournalLineRequest line(AccountEntity account, String description, BigDecimal debit, BigDecimal credit) {
        return CreateJournalLineRequest.builder()
                .accountId(Long.valueOf(account.getAccountId()))
                .description(description)
                .debitAmount(debit)
                .creditAmount(credit)
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
