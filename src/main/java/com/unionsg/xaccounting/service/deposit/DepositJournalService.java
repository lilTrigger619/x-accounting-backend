package com.unionsg.xaccounting.service.deposit;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.deposit.Deposit;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
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
import java.time.LocalDate;
import java.util.List;

/**
 * Posts every GL movement of a {@link Deposit} through the existing journal engine. Each method
 * branches on direction, so a deposit paid (asset) and a deposit received (liability) never
 * share an account:
 *
 * <pre>
 *                     DEPOSIT_PAID                         DEPOSIT_RECEIVED
 * activate     Dr Deposit asset   / Cr Bank         Dr Bank              / Cr Deposit liability
 * apply        Dr Accounts payable/ Cr Deposit asset Dr Deposit liability / Cr Accounts receivable
 * refund       Dr Bank            / Cr Deposit asset Dr Deposit liability / Cr Bank
 * forfeit      Dr Forfeit expense / Cr Deposit asset Dr Deposit liability / Cr Forfeit income
 * transfer     Dr Target asset    / Cr Source asset  Dr Source liability  / Cr Target liability
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class DepositJournalService {

    public static final String SOURCE_MODULE = "DEPOSIT";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    @Transactional
    public JournalEntry postActivation(Deposit deposit) {
        String depositAccount = depositAccountCode(deposit);
        String bank = bankAccountCode(deposit, null);
        boolean paid = deposit.getDirection() == DepositDirection.DEPOSIT_PAID;
        return post(deposit, deposit.getDepositDate(), "",
                (paid ? "Deposit paid to " : "Deposit received from ") + deposit.getCounterpartyName(),
                paid ? depositAccount : bank, paid ? bank : depositAccount, deposit.getAmount());
    }

    @Transactional
    public JournalEntry postApplication(Deposit deposit, BigDecimal amount, LocalDate date, String documentNumber, String suffix) {
        String depositAccount = depositAccountCode(deposit);
        if (deposit.getDirection() == DepositDirection.DEPOSIT_RECEIVED) {
            String receivable = mapped(MappingKey.INVOICE_ACCOUNTS_RECEIVABLE);
            return post(deposit, date, suffix, "Deposit applied to invoice " + documentNumber,
                    depositAccount, receivable, amount);
        }
        String payable = mapped(MappingKey.BILL_ACCOUNTS_PAYABLE);
        return post(deposit, date, suffix, "Deposit applied to bill " + documentNumber,
                payable, depositAccount, amount);
    }

    @Transactional
    public JournalEntry postRefund(Deposit deposit, BigDecimal amount, LocalDate date, BankAccount bankAccount, String suffix) {
        String depositAccount = depositAccountCode(deposit);
        String bank = bankAccountCode(deposit, bankAccount);
        if (deposit.getDirection() == DepositDirection.DEPOSIT_PAID) {
            return post(deposit, date, suffix, "Deposit refunded by " + deposit.getCounterpartyName(),
                    bank, depositAccount, amount);
        }
        return post(deposit, date, suffix, "Deposit refunded to " + deposit.getCounterpartyName(),
                depositAccount, bank, amount);
    }

    @Transactional
    public JournalEntry postForfeiture(Deposit deposit, BigDecimal amount, LocalDate date, AccountEntity override, String suffix) {
        String depositAccount = depositAccountCode(deposit);
        if (deposit.getDirection() == DepositDirection.DEPOSIT_PAID) {
            String expense = forfeitureAccountCode(deposit, override, MappingKey.DEPOSIT_FORFEIT_EXPENSE);
            return post(deposit, date, suffix, "Deposit forfeited to " + deposit.getCounterpartyName(),
                    expense, depositAccount, amount);
        }
        String income = forfeitureAccountCode(deposit, override, MappingKey.DEPOSIT_FORFEIT_INCOME);
        return post(deposit, date, suffix, "Deposit forfeited by " + deposit.getCounterpartyName(),
                depositAccount, income, amount);
    }

    @Transactional
    public JournalEntry postTransfer(Deposit source, Deposit target, BigDecimal amount, LocalDate date, String suffix) {
        String from = depositAccountCode(source);
        String to = depositAccountCode(target);
        String description = "Deposit transferred to " + target.getDepositNumber() + " (" + target.getCounterpartyName() + ")";
        if (source.getDirection() == DepositDirection.DEPOSIT_PAID) {
            return post(source, date, suffix, description, to, from, amount);
        }
        return post(source, date, suffix, description, from, to, amount);
    }

    /** Reverses a posted deposit journal with the journal engine's own reversal. */
    @Transactional
    public JournalEntry reverse(Deposit deposit, JournalEntry journal, String reason) {
        JournalResponse reversal = journalService.reverse(journal.getId(), reason);
        JournalEntry entry = journalEntryRepository.findById(reversal.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after reversal"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(deposit.getId());
        return journalEntryRepository.save(entry);
    }

    /** The account this deposit is held in: its own override, then its type's, then the direction's mapping. */
    public String depositAccountCode(Deposit deposit) {
        if (deposit.getDepositAccount() != null) {
            return deposit.getDepositAccount().getAccountId();
        }
        if (deposit.getDepositType() != null && deposit.getDepositType().getDepositAccount() != null) {
            return deposit.getDepositType().getDepositAccount().getAccountId();
        }
        return mapped(deposit.getDirection() == DepositDirection.DEPOSIT_PAID
                ? MappingKey.DEPOSIT_PAID_ASSET : MappingKey.DEPOSIT_RECEIVED_LIABILITY);
    }

    private String forfeitureAccountCode(Deposit deposit, AccountEntity override, MappingKey fallback) {
        if (override != null) {
            return override.getAccountId();
        }
        if (deposit.getDepositType() != null && deposit.getDepositType().getForfeitureAccount() != null) {
            return deposit.getDepositType().getForfeitureAccount().getAccountId();
        }
        return mapped(fallback);
    }

    private String bankAccountCode(Deposit deposit, BankAccount override) {
        BankAccount bank = override != null ? override : deposit.getBankAccount();
        if (bank != null && bank.getGlAccountCode() != null) {
            return bank.getGlAccountCode();
        }
        return mapped(MappingKey.DEPOSIT_BANK_ACCOUNT);
    }

    private String mapped(MappingKey key) {
        String code = accountingMappingService.resolve(key);
        if (!accountRepository.existsByAccountId(code)) {
            throw new BusinessException("Required accounting configuration missing: \"" + key.getDescription()
                    + "\" is mapped to account code \"" + code + "\", which does not exist in the Chart of Accounts. "
                    + "Configure it under Settings > Accounting Mappings.");
        }
        return code;
    }

    private JournalEntry post(Deposit deposit, LocalDate date, String suffix, String description,
                              String debitCode, String creditCode, BigDecimal amount) {
        Long debit = accountId(debitCode);
        Long credit = accountId(creditCode);
        String text = description + " - " + deposit.getDepositNumber();

        List<CreateJournalLineRequest> lines = List.of(
                CreateJournalLineRequest.builder().accountId(debit).description(text)
                        .debitAmount(amount).creditAmount(BigDecimal.ZERO).build(),
                CreateJournalLineRequest.builder().accountId(credit).description(text)
                        .debitAmount(BigDecimal.ZERO).creditAmount(amount).build()
        );

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(date)
                .reference(deposit.getDepositNumber() + suffix)
                .description(text + ".")
                .journalType(JournalType.DEPOSIT)
                .currencyCode(deposit.getCurrency())
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(deposit.getId());
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());
        return journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting"));
    }

    private Long accountId(String code) {
        return accountRepository.findByAccountId(code)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with code: " + code));
    }
}
