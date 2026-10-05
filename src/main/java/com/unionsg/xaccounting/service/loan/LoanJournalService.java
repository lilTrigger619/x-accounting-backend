package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Posts the GL impact of a {@link Loan} through the normal journal create/post path, so period
 * locks, numbering and balance checks all apply. Every posting branches on {@code direction}:
 *
 * <pre>
 * BORROWED_LOAN  disbursement  Dr Bank/Cash            Cr Loan Liability
 *                payment       Dr Loan Liability       Cr Bank/Cash
 *                              Dr Interest Expense (or Interest Payable for accrued interest)
 *                              Dr Loan Fees Expense
 * LENT_LOAN      disbursement  Dr Loan Receivable      Cr Bank/Cash
 *                payment       Dr Bank/Cash            Cr Loan Receivable
 *                                                      Cr Interest Income (or Interest Receivable)
 *                                                      Cr Loan Fee Income
 * </pre>
 *
 * Principal, interest and fees always land on separate lines so each is tracked on its own.
 */
@Service
@RequiredArgsConstructor
public class LoanJournalService {

    public static final String SOURCE_MODULE = "LOAN";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;
    private final PeriodLockGuard periodLockGuard;

    /**
     * Upfront fees: when EXPENSED_IMMEDIATELY they are netted off the cash; when CAPITALIZED they
     * are added to the loan balance. Either way they hit fee expense (borrowed) or fee income (lent).
     */
    @Transactional
    public JournalEntry postDisbursementJournal(Loan loan, LocalDate date) {
        Long principalAccountId = resolvePrincipalAccountId(loan);
        Long bankAccountId = resolveBankAccountId(loan);
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal fees = nz(loan.getTotalFees());
        boolean capitalized = fees.signum() > 0 && loan.getFeeTreatment() == LoanFeeTreatment.CAPITALIZED;
        BigDecimal balance = capitalized ? principal.add(fees) : principal;
        BigDecimal cash = capitalized ? principal : principal.subtract(fees);
        String no = loan.getLoanNumber();

        List<CreateJournalLineRequest> lines = new ArrayList<>();
        if (isBorrowed(loan)) {
            lines.add(line(bankAccountId, "Loan received: " + no, cash, BigDecimal.ZERO));
            if (fees.signum() > 0) {
                lines.add(line(resolveMappedAccountId(MappingKey.LOAN_FEE_EXPENSE), "Fees on loan " + no, fees, BigDecimal.ZERO));
            }
            lines.add(line(principalAccountId, "Loan liability: " + no, BigDecimal.ZERO, balance));
        } else {
            lines.add(line(principalAccountId, "Loan receivable: " + no, balance, BigDecimal.ZERO));
            lines.add(line(bankAccountId, "Loan disbursed: " + no, BigDecimal.ZERO, cash));
            if (fees.signum() > 0) {
                lines.add(line(resolveMappedAccountId(MappingKey.LOAN_FEE_INCOME), "Fees on loan " + no, BigDecimal.ZERO, fees));
            }
        }

        String description = "Loan " + no + (isBorrowed(loan) ? " received from " : " disbursed to ")
                + loan.getCounterpartyName() + ".";
        return createAndPost(loan, date, lines, "", description);
    }

    @Transactional
    public JournalEntry postRepaymentJournal(Loan loan, LoanRepayment p) {
        Long principalAccountId = resolvePrincipalAccountId(loan);
        Long bankAccountId = p.getBankAccount() != null
                ? resolveAccountId(p.getBankAccount().getGlAccountCode())
                : resolveBankAccountId(loan);
        String no = loan.getLoanNumber();

        BigDecimal principal = p.getPrincipalAmount().add(p.getOverpaymentAmount());
        BigDecimal accrued = p.getAccruedInterestApplied();
        BigDecimal interestNow = p.getInterestAmount().subtract(accrued);
        BigDecimal fees = p.getFeesAmount();
        BigDecimal total = p.getTotalAmount();

        List<CreateJournalLineRequest> lines = new ArrayList<>();
        if (isBorrowed(loan)) {
            if (principal.signum() > 0) {
                lines.add(line(principalAccountId, "Principal repaid: " + no, principal, BigDecimal.ZERO));
            }
            if (interestNow.signum() > 0) {
                lines.add(line(resolveInterestAccountId(loan), "Interest paid: " + no, interestNow, BigDecimal.ZERO));
            }
            if (accrued.signum() > 0) {
                lines.add(line(resolveMappedAccountId(MappingKey.LOAN_INTEREST_PAYABLE), "Accrued interest paid: " + no, accrued, BigDecimal.ZERO));
            }
            if (fees.signum() > 0) {
                lines.add(line(resolveMappedAccountId(MappingKey.LOAN_FEE_EXPENSE), "Fees paid: " + no, fees, BigDecimal.ZERO));
            }
            lines.add(line(bankAccountId, "Loan payment made: " + no, BigDecimal.ZERO, total));
        } else {
            lines.add(line(bankAccountId, "Loan payment received: " + no, total, BigDecimal.ZERO));
            if (principal.signum() > 0) {
                lines.add(line(principalAccountId, "Principal received: " + no, BigDecimal.ZERO, principal));
            }
            if (interestNow.signum() > 0) {
                lines.add(line(resolveInterestAccountId(loan), "Interest received: " + no, BigDecimal.ZERO, interestNow));
            }
            if (accrued.signum() > 0) {
                lines.add(line(resolveMappedAccountId(MappingKey.LOAN_INTEREST_RECEIVABLE), "Accrued interest received: " + no, BigDecimal.ZERO, accrued));
            }
            if (fees.signum() > 0) {
                lines.add(line(resolveMappedAccountId(MappingKey.LOAN_FEE_INCOME), "Fees received: " + no, BigDecimal.ZERO, fees));
            }
        }
        return createAndPost(loan, p.getRepaymentDate(), lines, "-P" + p.getId(), "Payment on loan " + no + ".");
    }

    /** BORROWED: Dr Interest Expense, Cr Interest Payable. LENT: Dr Interest Receivable, Cr Interest Income. */
    @Transactional
    public JournalEntry postAccrualJournal(Loan loan, BigDecimal amount, LocalDate date, Long accrualId) {
        Long interestAccountId = resolveInterestAccountId(loan);
        String no = loan.getLoanNumber();
        List<CreateJournalLineRequest> lines;
        if (isBorrowed(loan)) {
            Long payable = resolveMappedAccountId(MappingKey.LOAN_INTEREST_PAYABLE);
            lines = List.of(
                    line(interestAccountId, "Interest accrued: " + no, amount, BigDecimal.ZERO),
                    line(payable, "Interest accrued: " + no, BigDecimal.ZERO, amount));
        } else {
            Long receivable = resolveMappedAccountId(MappingKey.LOAN_INTEREST_RECEIVABLE);
            lines = List.of(
                    line(receivable, "Interest accrued: " + no, amount, BigDecimal.ZERO),
                    line(interestAccountId, "Interest accrued: " + no, BigDecimal.ZERO, amount));
        }
        return createAndPost(loan, date, lines, "-A" + accrualId, "Interest accrual for loan " + no + ".");
    }

    /**
     * Writes off what a lent loan's counterparty still owes: Dr Loan Write-off Expense for the
     * total, Cr Loan Receivable (principal) and Cr Interest Receivable (accrued interest).
     */
    @Transactional
    public JournalEntry postWriteOffJournal(Loan loan, BigDecimal principal, BigDecimal accruedInterest, LocalDate date) {
        if (isBorrowed(loan)) {
            throw new BusinessException("Only a loan the organization lent out can be written off");
        }
        String no = loan.getLoanNumber();
        List<CreateJournalLineRequest> lines = new ArrayList<>();
        lines.add(line(resolveMappedAccountId(MappingKey.LOAN_WRITE_OFF_EXPENSE), "Loan written off: " + no,
                principal.add(accruedInterest), BigDecimal.ZERO));
        if (principal.signum() > 0) {
            lines.add(line(resolvePrincipalAccountId(loan), "Loan written off: " + no, BigDecimal.ZERO, principal));
        }
        if (accruedInterest.signum() > 0) {
            lines.add(line(resolveMappedAccountId(MappingKey.LOAN_INTEREST_RECEIVABLE), "Accrued interest written off: " + no,
                    BigDecimal.ZERO, accruedInterest));
        }
        return createAndPost(loan, date, lines, "-WO", "Write-off of the unpaid balance of loan " + no + ".");
    }

    /** Posts the mirror image of a loan journal. The original is marked REVERSED, never edited. */
    @Transactional
    public JournalEntry reverse(JournalEntry journal, String reason) {
        JournalResponse reversal = journalService.reverse(journal.getId(), reason);
        JournalEntry entry = journalEntryRepository.findById(reversal.getId())
                .orElseThrow(() -> new BusinessException("Reversal journal not found after posting"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(journal.getSourceEntityId());
        return journalEntryRepository.save(entry);
    }

    private JournalEntry createAndPost(Loan loan, LocalDate date, List<CreateJournalLineRequest> lines,
                                       String referenceSuffix, String description) {
        periodLockGuard.assertPostable(date);
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(date)
                .reference(loan.getLoanNumber() + referenceSuffix)
                .description(description)
                .journalType(JournalType.LOAN)
                .currencyCode(loan.getCurrency())
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

    private CreateJournalLineRequest line(Long accountId, String description, BigDecimal debit, BigDecimal credit) {
        return CreateJournalLineRequest.builder()
                .accountId(accountId)
                .description(description)
                .debitAmount(debit)
                .creditAmount(credit)
                .build();
    }

    private static boolean isBorrowed(Loan loan) {
        return loan.getDirection() == LoanDirection.BORROWED_LOAN;
    }

    private Long resolvePrincipalAccountId(Loan loan) {
        if (loan.getPrincipalAccount() != null) {
            return resolveAccountId(loan.getPrincipalAccount().getAccountId());
        }
        return resolveMappedAccountId(isBorrowed(loan) ? MappingKey.LOAN_PAYABLE : MappingKey.LOAN_RECEIVABLE);
    }

    private Long resolveInterestAccountId(Loan loan) {
        if (loan.getInterestAccount() != null) {
            return resolveAccountId(loan.getInterestAccount().getAccountId());
        }
        return resolveMappedAccountId(isBorrowed(loan) ? MappingKey.LOAN_INTEREST_EXPENSE : MappingKey.LOAN_INTEREST_INCOME);
    }

    private Long resolveBankAccountId(Loan loan) {
        if (loan.getBankAccount() != null) {
            return resolveAccountId(loan.getBankAccount().getGlAccountCode());
        }
        return resolveMappedAccountId(MappingKey.LOAN_BANK_ACCOUNT);
    }

    private Long resolveAccountId(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with code: " + accountCode));
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

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
