package com.unionsg.xaccounting.service.expense;

import com.unionsg.xaccounting.dto.expense.ExpenseAccountingLine;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.expense.Expense;
import com.unionsg.xaccounting.entity.expense.ExpenseLine;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds and posts an expense's journal through the shared {@link JournalService}, and
 * reverses it the same way any posted journal is reversed. All amounts are in base currency.
 *
 * <pre>
 *   Dr expense account of each line    line amount
 *   Cr payment account's GL            total
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ExpenseJournalService {

    public static final String SOURCE_MODULE = "EXPENSE";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    /** The lines posting this expense would produce; nothing is saved. */
    @Transactional(readOnly = true)
    public List<ExpenseAccountingLine> buildLines(Expense expense) {
        // Unsaved previews have no number yet.
        String number = expense.getExpenseNumber() != null ? " " + expense.getExpenseNumber() : "";
        List<ExpenseAccountingLine> lines = new ArrayList<>();
        for (ExpenseLine line : expense.getLines()) {
            lines.add(ExpenseAccountingLine.builder()
                    .accountCode(line.getAccount().getAccountId())
                    .accountName(line.getAccount().getAccountName())
                    .description(truncate(line.getDescription(), 500))
                    .debit(line.getBaseAmount())
                    .credit(BigDecimal.ZERO)
                    .build());
        }
        String paymentCode = expense.getPaymentAccount().getGlAccountCode();
        AccountEntity paymentGl = accountRepository.findByAccountId(paymentCode)
                .orElseThrow(() -> new BusinessException("Payment account \"" + expense.getPaymentAccount().getAccountName()
                        + "\" posts to account " + paymentCode + ", which does not exist in the Chart of Accounts"));
        lines.add(ExpenseAccountingLine.builder()
                .accountCode(paymentGl.getAccountId())
                .accountName(paymentGl.getAccountName())
                .description(truncate("Expense" + number + " paid from " + expense.getPaymentAccount().getAccountName()
                        + (expense.getSupplier() != null ? " to " + expense.getSupplier().getDisplayName() : ""), 500))
                .debit(BigDecimal.ZERO)
                .credit(expense.getBaseTotalAmount())
                .build());
        return lines;
    }

    /** Creates and posts the expense's journal, tagged with the expense as its source. */
    @Transactional
    public JournalEntry postExpenseJournal(Expense expense) {
        List<CreateJournalLineRequest> lineRequests = buildLines(expense).stream()
                .map(line -> CreateJournalLineRequest.builder()
                        .accountId(Long.valueOf(line.getAccountCode()))
                        .description(line.getDescription())
                        .debitAmount(line.getDebit())
                        .creditAmount(line.getCredit())
                        .build())
                .toList();

        String description = "Expense " + expense.getExpenseNumber()
                + (expense.getSupplier() != null ? " - " + expense.getSupplier().getDisplayName() : "")
                + (expense.getMemo() != null && !expense.getMemo().isBlank() ? ": " + expense.getMemo() : "");

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(expense.getPaymentDate())
                .reference(expense.getExpenseNumber())
                .description(truncate(description, 500))
                .journalType(JournalType.EXPENSE)
                .currencyCode(expense.getBaseCurrency())
                .lines(lineRequests)
                .build();

        JournalResponse created = journalService.create(request);
        JournalEntry entry = loadJournal(created.getId());
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(expense.getId());
        entry.setSystemGenerated(true);
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());
        return loadJournal(posted.getId());
    }

    /** Posts the reversal journal for the expense's journal; the original stays untouched. */
    @Transactional
    public JournalEntry reverseExpenseJournal(Expense expense, String reason) {
        if (expense.getJournal() == null) {
            throw new BusinessException("Expense " + expense.getExpenseNumber() + " has no journal to reverse");
        }
        String description = "Reversal of expense " + expense.getExpenseNumber()
                + (reason != null && !reason.isBlank() ? ": " + reason : "");
        JournalResponse reversal = journalService.reverse(expense.getJournal().getId(), truncate(description, 500));

        JournalEntry entry = loadJournal(reversal.getId());
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(expense.getId());
        entry.setSystemGenerated(true);
        entry.setCurrencyCode(expense.getBaseCurrency());
        return journalEntryRepository.save(entry);
    }

    private JournalEntry loadJournal(Long id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Journal " + id + " not found"));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
