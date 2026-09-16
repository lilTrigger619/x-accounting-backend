package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.payroll.CreateSalaryAdvanceRequest;
import com.unionsg.xaccounting.dto.payroll.SalaryAdvanceResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.payroll.SalaryAdvance;
import com.unionsg.xaccounting.enums.AdvanceStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.PayrollAuditAction;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.payroll.SalaryAdvanceRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Salary advances (§14): Advance issued -> Employee receivable -> Payroll deduction -> Receivable
 * settlement. Exactly like {@link EmployeeLoanService}, the payroll deduction that recovers an
 * advance only adjusts {@code outstandingBalance} here; the GL impact posts once, at the Payroll
 * Run level (§49).
 */
@Service
@RequiredArgsConstructor
public class SalaryAdvanceService {

    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final EmployeeService employeeService;
    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final PayrollAuditLogService auditLogService;
    private final AccountingMappingService accountingMappingService;

    @Transactional
    public SalaryAdvanceResponse issue(CreateSalaryAdvanceRequest request) {
        Employee employee = employeeService.getEntity(request.getEmployeeId());

        SalaryAdvance advance = new SalaryAdvance();
        advance.setEmployee(employee);
        advance.setAmountIssued(request.getAmountIssued());
        advance.setDateIssued(request.getDateIssued());
        advance.setOutstandingBalance(request.getAmountIssued());
        advance.setRecurringDeductionAmount(request.getRecurringDeductionAmount());
        advance.setStatus(AdvanceStatus.OUTSTANDING);
        advance.setReason(request.getReason());

        List<CreateJournalLineRequest> lines = new ArrayList<>();
        lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_ADVANCE_RECEIVABLE)))
                .description("Salary advance issued to " + employee.getFullName())
                .debitAmount(request.getAmountIssued())
                .creditAmount(BigDecimal.ZERO)
                .build());
        lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_PAYMENT_BANK_ACCOUNT)))
                .description("Salary advance disbursement")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(request.getAmountIssued())
                .build());

        CreateJournalRequest journalRequest = CreateJournalRequest.builder()
                .journalDate(request.getDateIssued())
                .description("Salary advance issued to " + employee.getFullName() + " (" + employee.getEmployeeNumber() + ")")
                .journalType(JournalType.GENERAL)
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(journalRequest);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("SALARY_ADVANCE");
        entry.setSourceEntityId(advance.getId());
        journalEntryRepository.save(entry);
        JournalResponse posted = journalService.post(created.getId());

        advance.setIssuanceJournalId(posted.getId());
        SalaryAdvance saved = salaryAdvanceRepository.save(advance);

        auditLogService.record(PayrollAuditEntityType.SALARY_ADVANCE, saved.getId(), PayrollAuditAction.CREATED,
                null, "Issued " + request.getAmountIssued() + " to " + employee.getFullName(), request.getReason());

        return toResponse(saved);
    }

    /** Reduces the advance's outstanding balance for a payroll deduction. Posts no journal itself - see class doc. */
    @Transactional
    public void applyRepayment(Long advanceId, BigDecimal amount) {
        SalaryAdvance advance = getEntity(advanceId);
        BigDecimal newBalance = advance.getOutstandingBalance().subtract(amount).max(BigDecimal.ZERO);
        advance.setOutstandingBalance(newBalance);
        advance.setStatus(newBalance.compareTo(BigDecimal.ZERO) == 0
                ? AdvanceStatus.RECOVERED
                : AdvanceStatus.PARTIALLY_RECOVERED);
        salaryAdvanceRepository.save(advance);
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceResponse> getByEmployee(Long employeeId) {
        return salaryAdvanceRepository
                .findByEmployeeIdAndStatusIn(employeeId, List.of(AdvanceStatus.OUTSTANDING, AdvanceStatus.PARTIALLY_RECOVERED))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceResponse> getAllOutstanding() {
        return salaryAdvanceRepository.findByStatusIn(List.of(AdvanceStatus.OUTSTANDING, AdvanceStatus.PARTIALLY_RECOVERED))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalaryAdvance getEntity(Long id) {
        return salaryAdvanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary advance not found: " + id));
    }

    private Long resolveAccountId(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .map(a -> Long.valueOf(a.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountCode));
    }

    public SalaryAdvanceResponse toResponse(SalaryAdvance advance) {
        return SalaryAdvanceResponse.builder()
                .id(advance.getId())
                .employeeId(advance.getEmployee().getId())
                .employeeName(advance.getEmployee().getFullName())
                .amountIssued(advance.getAmountIssued())
                .dateIssued(advance.getDateIssued())
                .outstandingBalance(advance.getOutstandingBalance())
                .recurringDeductionAmount(advance.getRecurringDeductionAmount())
                .status(advance.getStatus())
                .issuanceJournalId(advance.getIssuanceJournalId())
                .reason(advance.getReason())
                .build();
    }
}
