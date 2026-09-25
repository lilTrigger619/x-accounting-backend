package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Posts the GL impact of a {@link Loan} at disbursement, each repayment, interest accrual, and
 * write-off (Loans spec §15/§16/§20). Every posting branches on {@code direction}: a BORROWED
 * loan hits Loan Payable/Interest Expense/Interest Payable, a LENT loan hits Loan Receivable/
 * Interest Income/Interest Receivable - the two are never posted to the same accounts, so the
 * organization's own borrowing never gets mixed into money it has lent out, or vice versa (§15's
 * "accounting direction depends on whether the organization is the borrower or lender").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanJournalService {

    public static final String SOURCE_MODULE = "LOAN";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    /**
     * BORROWED: Dr Bank, Cr Loan Payable (+ Dr Loan Fee Expense / less net cash, when fees are
     * expensed immediately at disbursement).
     * LENT: Dr Loan Receivable, Cr Bank.
     */
    @Transactional
    public JournalEntry postDisbursementJournal(Loan loan) {
        Long principalAccountId = resolvePrincipalAccountId(loan);
        Long bankAccountId = resolveBankAccountId(loan);
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal fees = loan.getTotalFees() != null ? loan.getTotalFees() : BigDecimal.ZERO;
        boolean expenseFeesNow = fees.compareTo(BigDecimal.ZERO) > 0
                && loan.getFeeTreatment() != null
                && loan.getFeeTreatment() == com.unionsg.xaccounting.enums.loan.LoanFeeTreatment.EXPENSED_IMMEDIATELY;

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        if (loan.getDirection() == LoanDirection.BORROWED) {
            BigDecimal netCash = expenseFeesNow ? principal.subtract(fees) : principal;

            lines.add(line(bankAccountId, "Loan disbursed: " + loan.getLoanNumber(), netCash, BigDecimal.ZERO));
            if (expenseFeesNow) {
                Long feeAccountId = resolveMappedAccountId(MappingKey.LOAN_FEE_EXPENSE);
                lines.add(line(feeAccountId, "Fees on loan " + loan.getLoanNumber(), fees, BigDecimal.ZERO));
            }
            lines.add(line(principalAccountId, "Loan payable for " + loan.getLoanNumber(), BigDecimal.ZERO, principal));
        } else {
            lines.add(line(principalAccountId, "Loan disbursed: " + loan.getLoanNumber(), principal, BigDecimal.ZERO));
            lines.add(line(bankAccountId, "Loan disbursed: " + loan.getLoanNumber(), BigDecimal.ZERO, principal));
        }

        String description = "Loan " + loan.getLoanNumber() + " disbursed "
                + (loan.getDirection() == LoanDirection.BORROWED ? "from " : "to ")
                + loan.getCounterpartyName() + ".";

        return createAndPostJournal(loan, loan.getStartDate(), lines, "", description);
    }

    /**
     * BORROWED: Dr Loan Payable (principal) / Dr Interest Expense (interest) / Dr Loan Fee
     * Expense (fees), Cr Bank (total).
     * LENT: Dr Bank (total), Cr Loan Receivable (principal) / Cr Interest Income (interest +
     * fees, since lending-side fee income has no dedicated account this pass).
     */
    @Transactional
    public JournalEntry postRepaymentJournal(Loan loan, LoanRepayment repayment) {
        Long principalAccountId = resolvePrincipalAccountId(loan);
        Long bankAccountId = resolveRepaymentBankAccountId(loan, repayment);

        BigDecimal principal = repayment.getPrincipalAmount();
        BigDecimal interest = repayment.getInterestAmount();
        BigDecimal fees = repayment.getFeesAmount();
        BigDecimal total = repayment.getTotalAmount();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        if (loan.getDirection() == LoanDirection.BORROWED) {
            if (principal.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(line(principalAccountId, "Principal repayment for " + loan.getLoanNumber(), principal, BigDecimal.ZERO));
            }
            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                Long interestAccountId = resolveInterestAccountId(loan);
                lines.add(line(interestAccountId, "Interest paid on " + loan.getLoanNumber(), interest, BigDecimal.ZERO));
            }
            if (fees.compareTo(BigDecimal.ZERO) > 0) {
                Long feeAccountId = resolveMappedAccountId(MappingKey.LOAN_FEE_EXPENSE);
                lines.add(line(feeAccountId, "Fees paid on " + loan.getLoanNumber(), fees, BigDecimal.ZERO));
            }
            lines.add(line(bankAccountId, "Repayment made: " + loan.getLoanNumber(), BigDecimal.ZERO, total));
        } else {
            lines.add(line(bankAccountId, "Repayment received: " + loan.getLoanNumber(), total, BigDecimal.ZERO));
            if (principal.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(line(principalAccountId, "Principal received for " + loan.getLoanNumber(), BigDecimal.ZERO, principal));
            }
            BigDecimal incomePortion = interest.add(fees);
            if (incomePortion.compareTo(BigDecimal.ZERO) > 0) {
                Long interestAccountId = resolveInterestAccountId(loan);
                lines.add(line(interestAccountId, "Interest received on " + loan.getLoanNumber(), BigDecimal.ZERO, incomePortion));
            }
        }

        String description = "Repayment on loan " + loan.getLoanNumber() + ".";

        return createAndPostJournal(loan, repayment.getRepaymentDate(), lines,
                "-R" + repayment.getId(), description);
    }

    /**
     * Interest accrued before payment (§20).
     * BORROWED: Dr Interest Expense, Cr Interest Payable.
     * LENT: Dr Interest Receivable, Cr Interest Income.
     */
    @Transactional
    public JournalEntry postAccrualJournal(Loan loan, BigDecimal amount, LocalDate date) {
        Long interestAccountId = resolveInterestAccountId(loan);
        Long accrualAccountId = resolveMappedAccountId(
                loan.getDirection() == LoanDirection.BORROWED
                        ? MappingKey.LOAN_INTEREST_PAYABLE
                        : MappingKey.LOAN_INTEREST_RECEIVABLE
        );

        List<CreateJournalLineRequest> lines;
        if (loan.getDirection() == LoanDirection.BORROWED) {
            lines = List.of(
                    line(interestAccountId, "Interest accrued on " + loan.getLoanNumber(), amount, BigDecimal.ZERO),
                    line(accrualAccountId, "Interest accrued on " + loan.getLoanNumber(), BigDecimal.ZERO, amount)
            );
        } else {
            lines = List.of(
                    line(accrualAccountId, "Interest accrued on " + loan.getLoanNumber(), amount, BigDecimal.ZERO),
                    line(interestAccountId, "Interest accrued on " + loan.getLoanNumber(), BigDecimal.ZERO, amount)
            );
        }

        String description = "Interest accrual for loan " + loan.getLoanNumber() + ".";

        return createAndPostJournal(loan, date, lines, "-ACCR-" + date, description);
    }

    /**
     * Write-off of a LENT loan's remaining outstanding principal (§21). BORROWED loans are not
     * supported here - forgiving a debt the organization itself owes is a distinct, formal
     * debt-restructuring event with its own gain recognition, out of scope this pass.
     */
    @Transactional
    public JournalEntry postWriteOffJournal(Loan loan, BigDecimal amount, LocalDate date) {
        if (loan.getDirection() != LoanDirection.LENT) {
            throw new BusinessException("Only a loan the organization lent out can be written off");
        }
        Long principalAccountId = resolvePrincipalAccountId(loan);
        Long feeAccountId = resolveMappedAccountId(MappingKey.LOAN_FEE_EXPENSE);

        List<CreateJournalLineRequest> lines = List.of(
                line(feeAccountId, "Loan written off: " + loan.getLoanNumber(), amount, BigDecimal.ZERO),
                line(principalAccountId, "Loan written off: " + loan.getLoanNumber(), BigDecimal.ZERO, amount)
        );

        String description = "Write-off of remaining balance for loan " + loan.getLoanNumber() + ".";

        return createAndPostJournal(loan, date, lines, "-WO", description);
    }

    private CreateJournalLineRequest line(Long accountId, String description, BigDecimal debit, BigDecimal credit) {
        return CreateJournalLineRequest.builder()
                .accountId(accountId)
                .description(description)
                .debitAmount(debit)
                .creditAmount(credit)
                .build();
    }

    private JournalEntry createAndPostJournal(
            Loan loan, LocalDate date, List<CreateJournalLineRequest> lines,
            String referenceSuffix, String description
    ) {
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(date)
                .reference(loan.getLoanNumber() + referenceSuffix)
                .description(description)
                .journalType(JournalType.LOAN)
                .currencyCode(loan.getCurrency() != null ? loan.getCurrency() : "USD")
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);

        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(loan.getId());
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());

        return journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting"));
    }

    private Long resolvePrincipalAccountId(Loan loan) {
        if (loan.getPrincipalAccount() != null) {
            return resolveAccountId(loan.getPrincipalAccount().getAccountId());
        }
        return resolveMappedAccountId(loan.getDirection() == LoanDirection.BORROWED
                ? MappingKey.LOAN_PAYABLE : MappingKey.LOAN_RECEIVABLE);
    }

    private Long resolveInterestAccountId(Loan loan) {
        if (loan.getInterestAccount() != null) {
            return resolveAccountId(loan.getInterestAccount().getAccountId());
        }
        return resolveMappedAccountId(loan.getDirection() == LoanDirection.BORROWED
                ? MappingKey.LOAN_INTEREST_EXPENSE : MappingKey.LOAN_INTEREST_INCOME);
    }

    private Long resolveBankAccountId(Loan loan) {
        if (loan.getBankAccount() != null) {
            return resolveAccountId(loan.getBankAccount().getGlAccountCode());
        }
        return resolveMappedAccountId(MappingKey.LOAN_BANK_ACCOUNT);
    }

    private Long resolveRepaymentBankAccountId(Loan loan, LoanRepayment repayment) {
        if (repayment.getBankAccount() != null) {
            return resolveAccountId(repayment.getBankAccount().getGlAccountCode());
        }
        return resolveBankAccountId(loan);
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
