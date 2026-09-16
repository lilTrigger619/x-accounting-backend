package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.payroll.RecordStatutoryPaymentRequest;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.StatutoryScheme;
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
import java.util.List;

/**
 * Records remittance of a withheld/accrued statutory liability to the scheme itself (§29). This
 * is deliberately independent of any single Payroll Run - statutory remittances are typically
 * batched across a whole period's payroll (e.g. one monthly pension filing covering every run
 * that period), and this liability must stay on the books, unsettled, until this is recorded.
 * Payroll posting alone never clears it (§29).
 */
@Service
@RequiredArgsConstructor
public class StatutoryPaymentService {

    private final StatutorySchemeService statutorySchemeService;
    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    @Transactional
    public JournalResponse recordPayment(RecordStatutoryPaymentRequest request) {
        StatutoryScheme scheme = statutorySchemeService.getEntity(request.getStatutorySchemeId());
        String liabilityAccountCode = request.isEmployeePortion()
                ? scheme.getEmployeeLiabilityAccountCode()
                : scheme.getEmployerLiabilityAccountCode();
        if (liabilityAccountCode == null || liabilityAccountCode.isBlank()) {
            throw new BusinessException("Statutory scheme " + scheme.getCode() + " has no liability account configured for the "
                    + (request.isEmployeePortion() ? "employee" : "employer") + " portion");
        }

        List<CreateJournalLineRequest> lines = List.of(
                CreateJournalLineRequest.builder()
                        .accountId(resolveAccountId(liabilityAccountCode))
                        .description(scheme.getName() + " remittance")
                        .debitAmount(request.getAmount())
                        .creditAmount(BigDecimal.ZERO)
                        .build(),
                CreateJournalLineRequest.builder()
                        .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_PAYMENT_BANK_ACCOUNT)))
                        .description(scheme.getName() + " remittance payment")
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(request.getAmount())
                        .build()
        );

        CreateJournalRequest journalRequest = CreateJournalRequest.builder()
                .journalDate(request.getPaymentDate())
                .description("Statutory remittance: " + scheme.getName())
                .journalType(JournalType.GENERAL)
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(journalRequest);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("STATUTORY_PAYMENT");
        entry.setSourceEntityId(scheme.getId());
        journalEntryRepository.save(entry);

        return journalService.post(created.getId());
    }

    private Long resolveAccountId(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .map(a -> Long.valueOf(a.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountCode));
    }
}
