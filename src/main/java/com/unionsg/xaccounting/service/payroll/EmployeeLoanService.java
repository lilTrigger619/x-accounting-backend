package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.payroll.CreateEmployeeLoanRequest;
import com.unionsg.xaccounting.dto.payroll.EmployeeLoanResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.payroll.EmployeeLoan;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.LoanStatus;
import com.unionsg.xaccounting.enums.PayrollAuditAction;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeLoanRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee loans (§15). Disbursement is booked as an asset (a receivable), never an expense - the
 * loan is the company's money lent out, not spent. Repayment through payroll only ever adjusts
 * this receivable's balance ({@link #applyRepayment}); the GL impact of that repayment is posted
 * exactly once, as part of the aggregate Payroll Run journal, so a loan is never expensed twice
 * (§49).
 */
@Service
@RequiredArgsConstructor
public class EmployeeLoanService {

    private final EmployeeLoanRepository employeeLoanRepository;
    private final EmployeeService employeeService;
    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final PayrollAuditLogService auditLogService;
    private final AccountingMappingService accountingMappingService;

    @Transactional
    public EmployeeLoanResponse disburse(CreateEmployeeLoanRequest request) {
        Employee employee = employeeService.getEntity(request.getEmployeeId());

        EmployeeLoan loan = new EmployeeLoan();
        loan.setEmployee(employee);
        loan.setPrincipal(request.getPrincipal());
        loan.setInterestRatePercent(request.getInterestRatePercent() != null ? request.getInterestRatePercent() : BigDecimal.ZERO);
        loan.setStartDate(request.getStartDate());
        loan.setEndDate(request.getEndDate());
        loan.setInstallmentAmount(request.getInstallmentAmount());
        loan.setOutstandingPrincipal(request.getPrincipal());
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setReason(request.getReason());

        List<CreateJournalLineRequest> lines = new ArrayList<>();
        lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_LOAN_RECEIVABLE)))
                .description("Loan disbursed to " + employee.getFullName())
                .debitAmount(request.getPrincipal())
                .creditAmount(BigDecimal.ZERO)
                .build());
        lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_PAYMENT_BANK_ACCOUNT)))
                .description("Loan disbursement")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(request.getPrincipal())
                .build());

        CreateJournalRequest journalRequest = CreateJournalRequest.builder()
                .journalDate(request.getStartDate())
                .description("Employee loan disbursed to " + employee.getFullName() + " (" + employee.getEmployeeNumber() + ")")
                .journalType(JournalType.GENERAL)
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(journalRequest);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("EMPLOYEE_LOAN");
        entry.setSourceEntityId(loan.getId());
        journalEntryRepository.save(entry);
        JournalResponse posted = journalService.post(created.getId());

        loan.setDisbursementJournalId(posted.getId());
        EmployeeLoan saved = employeeLoanRepository.save(loan);

        auditLogService.record(PayrollAuditEntityType.EMPLOYEE_LOAN, saved.getId(), PayrollAuditAction.CREATED,
                null, "Disbursed " + request.getPrincipal() + " to " + employee.getFullName(), request.getReason());

        return toResponse(saved);
    }

    /** Reduces the loan's outstanding balance for a payroll deduction. Posts no journal itself - see class doc. */
    @Transactional
    public void applyRepayment(Long loanId, BigDecimal principalPortion, BigDecimal interestPortion) {
        EmployeeLoan loan = getEntity(loanId);
        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().subtract(principalPortion).max(BigDecimal.ZERO));
        loan.setOutstandingInterest(loan.getOutstandingInterest().subtract(interestPortion).max(BigDecimal.ZERO));
        if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0
                && loan.getOutstandingInterest().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        employeeLoanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public List<EmployeeLoanResponse> getByEmployee(Long employeeId) {
        return employeeLoanRepository.findByEmployeeIdAndStatus(employeeId, LoanStatus.ACTIVE).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeLoanResponse> getAllActive() {
        return employeeLoanRepository.findByStatus(LoanStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeLoan getEntity(Long id) {
        return employeeLoanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee loan not found: " + id));
    }

    private Long resolveAccountId(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .map(a -> Long.valueOf(a.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountCode));
    }

    public EmployeeLoanResponse toResponse(EmployeeLoan loan) {
        return EmployeeLoanResponse.builder()
                .id(loan.getId())
                .employeeId(loan.getEmployee().getId())
                .employeeName(loan.getEmployee().getFullName())
                .principal(loan.getPrincipal())
                .interestRatePercent(loan.getInterestRatePercent())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .installmentAmount(loan.getInstallmentAmount())
                .outstandingPrincipal(loan.getOutstandingPrincipal())
                .outstandingInterest(loan.getOutstandingInterest())
                .status(loan.getStatus())
                .disbursementJournalId(loan.getDisbursementJournalId())
                .reason(loan.getReason())
                .build();
    }
}
