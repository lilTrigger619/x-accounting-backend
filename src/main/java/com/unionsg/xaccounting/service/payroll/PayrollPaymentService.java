package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Settles a POSTED Payroll Run's Salary Payable liability in cash (§27). This is a distinct
 * accounting event from posting (§26): posting recognized the expense and the liability; this
 * only ever moves Salary Payable to Bank. It must never re-debit a salary expense account - doing
 * so would double the cost of payroll (§49).
 */
@Service
@RequiredArgsConstructor
public class PayrollPaymentService {

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Value("${payroll.journal.salary-payable-account-id}")
    private String salaryPayableAccountId;

    @Value("${payroll.payment.bank-account-id}")
    private String bankAccountId;

    @Transactional
    public JournalResponse payRun(PayrollRun run, LocalDate paymentDate) {
        BigDecimal amount = run.getTotalNetPay();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payroll run " + run.getRunNumber() + " has no net pay to disburse");
        }

        List<CreateJournalLineRequest> lines = List.of(
                CreateJournalLineRequest.builder()
                        .accountId(resolveAccountId(salaryPayableAccountId))
                        .description("Salary payable settled for run " + run.getRunNumber())
                        .debitAmount(amount)
                        .creditAmount(BigDecimal.ZERO)
                        .build(),
                CreateJournalLineRequest.builder()
                        .accountId(resolveAccountId(bankAccountId))
                        .description("Payroll disbursement for run " + run.getRunNumber())
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(amount)
                        .build()
        );

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(paymentDate)
                .reference(run.getRunNumber() + "-PMT")
                .description("Payroll payment for run " + run.getRunNumber())
                .journalType(JournalType.GENERAL)
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("PAYROLL_PAYMENT");
        entry.setSourceEntityId(run.getId());
        journalEntryRepository.save(entry);

        return journalService.post(created.getId());
    }

    private Long resolveAccountId(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .map(a -> Long.valueOf(a.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountCode));
    }
}
