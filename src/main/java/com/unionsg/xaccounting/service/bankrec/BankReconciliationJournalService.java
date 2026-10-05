package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliation;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAdjustment;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Posts reconciliation adjustments through the standard journal pipeline (numbering, balance
 * and period-lock checks). Matching never posts anything: only adjustments create journals.
 *
 * <pre>
 *   money in  (interest, direct credit, unknown credit): Dr Bank,          Cr offset account
 *   money out (charges, direct debit, unknown debit):    Dr offset account, Cr Bank
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class BankReconciliationJournalService {

    public static final String SOURCE_MODULE = "BANK_RECONCILIATION";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional
    public JournalEntry postAdjustment(BankReconciliation reconciliation, BankReconciliationAdjustment adjustment) {
        String bankCode = reconciliation.getBankAccount().getGlAccountCode();
        String offsetCode = adjustment.getOffsetAccount().getAccountId();
        BigDecimal amount = adjustment.getAmount();
        String text = adjustment.getAdjustmentType().getLabel()
                + (adjustment.getDescription() != null ? ": " + adjustment.getDescription() : "");

        List<CreateJournalLineRequest> lines = adjustment.getAdjustmentType().isMoneyIn()
                ? List.of(line(bankCode, text, amount, BigDecimal.ZERO), line(offsetCode, text, BigDecimal.ZERO, amount))
                : List.of(line(offsetCode, text, amount, BigDecimal.ZERO), line(bankCode, text, BigDecimal.ZERO, amount));

        return createAndPost(reconciliation, adjustment, lines,
                reconciliation.getReconciliationNumber() + "-ADJ-" + adjustment.getId(),
                "Bank reconciliation " + reconciliation.getReconciliationNumber() + " adjustment. " + text);
    }

    /**
     * Posts the mirror image of an adjustment's journal on the adjustment's own date, so the
     * reversal falls in the same reconciliation period, and marks the original as reversed.
     */
    @Transactional
    public JournalEntry reverseAdjustment(BankReconciliation reconciliation, BankReconciliationAdjustment adjustment, String reason) {
        JournalEntry original = adjustment.getJournal();
        if (original == null || original.getStatus() != JournalStatus.POSTED) {
            throw new BusinessException("The adjustment's journal is not posted, so it cannot be reversed");
        }
        List<CreateJournalLineRequest> lines = original.getLines().stream()
                .map(l -> line(l.getAccount().getAccountId(), "Reversal of " + original.getJournalNumber(),
                        l.getCreditAmount(), l.getDebitAmount()))
                .toList();

        JournalEntry reversal = createAndPost(reconciliation, adjustment, lines,
                "REV-" + original.getJournalNumber(),
                "Reversal of bank reconciliation adjustment " + original.getJournalNumber() + ". " + reason);
        reversal.setReversalOfJournalId(original.getId());
        journalEntryRepository.save(reversal);

        original.setStatus(JournalStatus.REVERSED);
        original.setReversedAt(LocalDateTime.now());
        journalEntryRepository.save(original);
        return reversal;
    }

    /** The journal's line on the bank's GL account: the book transaction a match links to. */
    public static JournalLine bankLine(JournalEntry journal, String bankGlCode) {
        return journal.getLines().stream()
                .filter(l -> bankGlCode.equals(l.getAccount().getAccountId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Journal " + journal.getJournalNumber() + " has no bank line"));
    }

    private JournalEntry createAndPost(BankReconciliation reconciliation, BankReconciliationAdjustment adjustment,
                                       List<CreateJournalLineRequest> lines, String reference, String description) {
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(adjustment.getTransactionDate())
                .reference(reference)
                .description(description)
                .journalType(JournalType.ADJUSTMENT)
                .currencyCode(reconciliation.getCurrency() != null ? reconciliation.getCurrency() : "GHS")
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(reconciliation.getId());
        entry.setAdjustmentEntry(true);
        entry.setSystemGenerated(true);
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());
        return journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting"));
    }

    /** Journal lines take the account code as a number, as every other posting service here does. */
    private static CreateJournalLineRequest line(String accountCode, String description, BigDecimal debit, BigDecimal credit) {
        Long accountId;
        try {
            accountId = Long.valueOf(accountCode);
        } catch (NumberFormatException e) {
            throw new BusinessException("Account code \"" + accountCode + "\" cannot be posted to");
        }
        return CreateJournalLineRequest.builder()
                .accountId(accountId)
                .description(description)
                .debitAmount(debit)
                .creditAmount(credit)
                .build();
    }
}
