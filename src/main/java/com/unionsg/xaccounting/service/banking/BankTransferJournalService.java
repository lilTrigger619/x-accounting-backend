package com.unionsg.xaccounting.service.banking;

import com.unionsg.xaccounting.dto.banking.BankTransferAccountingLine;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.banking.BankTransfer;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds and posts a bank transfer's journal through the shared {@link JournalService}, and
 * reverses it the same way any posted journal is reversed. All amounts are in base currency.
 *
 * <pre>
 *   Dr destination bank GL     base value received
 *   Cr source bank GL          base value sent
 *   Dr bank charges            charges          (when there are charges)
 *   Cr source bank GL          charges
 *   Cr FX gain / Dr FX loss    difference       (cross-currency only)
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class BankTransferJournalService {

    public static final String SOURCE_MODULE = "BANK_TRANSFER";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    /** The lines posting this transfer would produce; nothing is saved. */
    @Transactional
    public List<BankTransferAccountingLine> buildLines(BankTransfer transfer) {
        // Unsaved previews have no number yet.
        String number = transfer.getTransferNumber() != null ? " " + transfer.getTransferNumber() : "";
        String sourceName = transfer.getSourceBankAccount().getAccountName();
        String destinationName = transfer.getDestinationBankAccount().getAccountName();

        List<BankTransferAccountingLine> lines = new ArrayList<>();
        addDebit(lines, transfer.getDestinationBankAccount().getGlAccountCode(),
                "Transfer" + number + " from " + sourceName, transfer.getBaseConvertedAmount());
        addCredit(lines, transfer.getSourceBankAccount().getGlAccountCode(),
                "Transfer" + number + " to " + destinationName, transfer.getBaseAmount());

        if (positive(transfer.getBaseFeeAmount())) {
            String feeAccountCode = transfer.getFeeAccount() != null
                    ? transfer.getFeeAccount().getAccountId()
                    : mappedCode(MappingKey.BANK_TRANSFER_CHARGES);
            addDebit(lines, feeAccountCode, "Bank charges on transfer" + number, transfer.getBaseFeeAmount());
            addCredit(lines, transfer.getSourceBankAccount().getGlAccountCode(),
                    "Bank charges on transfer" + number, transfer.getBaseFeeAmount());
        }

        BigDecimal gainLoss = transfer.getExchangeGainLoss() == null ? BigDecimal.ZERO : transfer.getExchangeGainLoss();
        if (gainLoss.signum() > 0) {
            addCredit(lines, mappedCode(MappingKey.FX_GAIN), "Exchange gain on transfer" + number, gainLoss);
        } else if (gainLoss.signum() < 0) {
            addDebit(lines, mappedCode(MappingKey.FX_LOSS), "Exchange loss on transfer" + number, gainLoss.negate());
        }
        return lines;
    }

    /** Creates and posts the transfer's journal, tagged with the transfer as its source. */
    @Transactional
    public JournalEntry postTransferJournal(BankTransfer transfer) {
        List<CreateJournalLineRequest> lineRequests = buildLines(transfer).stream()
                .map(line -> CreateJournalLineRequest.builder()
                        .accountId(Long.valueOf(line.getAccountCode()))
                        .description(line.getDescription())
                        .debitAmount(line.getDebit())
                        .creditAmount(line.getCredit())
                        .build())
                .toList();

        String description = "Bank transfer " + transfer.getTransferNumber() + ": "
                + transfer.getSourceBankAccount().getAccountName() + " to "
                + transfer.getDestinationBankAccount().getAccountName()
                + (transfer.getDescription() != null && !transfer.getDescription().isBlank()
                        ? " - " + transfer.getDescription() : "");

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(transfer.getTransferDate())
                .reference(transfer.getTransferNumber())
                .description(truncate(description, 500))
                .journalType(JournalType.BANK_TRANSFER)
                .currencyCode(transfer.getBaseCurrency())
                .lines(lineRequests)
                .build();

        JournalResponse created = journalService.create(request);
        JournalEntry entry = loadJournal(created.getId());
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(transfer.getId());
        entry.setSystemGenerated(true);
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());
        return loadJournal(posted.getId());
    }

    /** Posts the reversal journal for the transfer's journal; the original stays untouched. */
    @Transactional
    public JournalEntry reverseTransferJournal(BankTransfer transfer, String reason) {
        if (transfer.getJournal() == null) {
            throw new BusinessException("Transfer " + transfer.getTransferNumber() + " has no journal to reverse");
        }
        String description = "Reversal of bank transfer " + transfer.getTransferNumber()
                + (reason != null && !reason.isBlank() ? ": " + reason : "");
        JournalResponse reversal = journalService.reverse(transfer.getJournal().getId(), truncate(description, 500));

        JournalEntry entry = loadJournal(reversal.getId());
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(transfer.getId());
        entry.setSystemGenerated(true);
        entry.setCurrencyCode(transfer.getBaseCurrency());
        return journalEntryRepository.save(entry);
    }

    private void addDebit(List<BankTransferAccountingLine> lines, String code, String description, BigDecimal amount) {
        if (positive(amount)) {
            lines.add(line(code, description, amount, BigDecimal.ZERO));
        }
    }

    private void addCredit(List<BankTransferAccountingLine> lines, String code, String description, BigDecimal amount) {
        if (positive(amount)) {
            lines.add(line(code, description, BigDecimal.ZERO, amount));
        }
    }

    private BankTransferAccountingLine line(String code, String description, BigDecimal debit, BigDecimal credit) {
        AccountEntity account = accountRepository.findByAccountId(code)
                .orElseThrow(() -> new BusinessException(
                        "Account " + code + " does not exist in the Chart of Accounts"));
        return BankTransferAccountingLine.builder()
                .accountCode(account.getAccountId())
                .accountName(account.getAccountName())
                .description(truncate(description, 500))
                .debit(debit)
                .credit(credit)
                .build();
    }

    private String mappedCode(MappingKey key) {
        String code = accountingMappingService.resolve(key);
        if (!accountRepository.existsByAccountId(code)) {
            throw new BusinessException(
                    "Required accounting configuration missing: \"" + key.getDescription() + "\" is mapped "
                            + "to account code \"" + code + "\", which does not exist in the Chart of Accounts. "
                            + "Configure it under Settings > Accounting Mappings.");
        }
        return code;
    }

    private JournalEntry loadJournal(Long id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Journal " + id + " not found"));
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
