package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.payroll.CreateReimbursementClaimRequest;
import com.unionsg.xaccounting.dto.payroll.ReimbursementClaimResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.payroll.ReimbursementClaim;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.ReimbursementStatus;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.payroll.ReimbursementClaimRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee expense reimbursement claims (§16): Claim -> Approval -> Payment -> Accounting.
 *
 * <p>Approving a claim recognizes the expense and an Employee Reimbursements Payable liability -
 * <em>unless</em> {@code alreadyRecordedElsewhere} is set, meaning the underlying cost was already
 * booked through some other document (a Bill, a direct Expense entry). In that case this claim
 * posts no journal of its own at approval; recording the expense a second time here would
 * duplicate it (§16, §49) - the claim exists purely so the reimbursement is tracked and its
 * eventual payment is not lost, and that payment must flow through whatever process already
 * recorded the expense.</p>
 */
@Service
@RequiredArgsConstructor
public class ReimbursementClaimService {

    private final ReimbursementClaimRepository reimbursementClaimRepository;
    private final EmployeeService employeeService;
    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    @Transactional
    public ReimbursementClaimResponse submit(CreateReimbursementClaimRequest request) {
        Employee employee = employeeService.getEntity(request.getEmployeeId());
        ReimbursementClaim claim = new ReimbursementClaim();
        claim.setEmployee(employee);
        claim.setDescription(request.getDescription());
        claim.setCategory(request.getCategory());
        claim.setAmount(request.getAmount());
        claim.setClaimDate(request.getClaimDate());
        claim.setAlreadyRecordedElsewhere(request.isAlreadyRecordedElsewhere());
        claim.setStatus(ReimbursementStatus.PENDING);
        return toResponse(reimbursementClaimRepository.save(claim));
    }

    @Transactional
    public ReimbursementClaimResponse approve(Long id) {
        ReimbursementClaim claim = getEntity(id);
        if (claim.getStatus() != ReimbursementStatus.PENDING) {
            throw new BusinessException("Only a pending claim can be approved");
        }
        claim.setStatus(ReimbursementStatus.APPROVED);
        claim.setApprovedBy(SecurityUtils.getCurrentUser());
        claim.setApprovedAt(LocalDateTime.now());

        if (!claim.isAlreadyRecordedElsewhere()) {
            postApprovalJournal(claim);
        }

        return toResponse(reimbursementClaimRepository.save(claim));
    }

    @Transactional
    public ReimbursementClaimResponse reject(Long id) {
        ReimbursementClaim claim = getEntity(id);
        if (claim.getStatus() != ReimbursementStatus.PENDING) {
            throw new BusinessException("Only a pending claim can be rejected");
        }
        claim.setStatus(ReimbursementStatus.REJECTED);
        return toResponse(reimbursementClaimRepository.save(claim));
    }

    /** Settles the payable created at approval. No-op journal-wise when alreadyRecordedElsewhere (§49). */
    @Transactional
    public ReimbursementClaimResponse markPaid(Long id) {
        ReimbursementClaim claim = getEntity(id);
        if (claim.getStatus() != ReimbursementStatus.APPROVED) {
            throw new BusinessException("Only an approved claim can be marked paid");
        }
        if (!claim.isAlreadyRecordedElsewhere()) {
            List<CreateJournalLineRequest> lines = new ArrayList<>();
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_REIMBURSEMENT_PAYABLE)))
                    .description("Reimbursement paid to " + claim.getEmployee().getFullName())
                    .debitAmount(claim.getAmount())
                    .creditAmount(BigDecimal.ZERO)
                    .build());
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_PAYMENT_BANK_ACCOUNT)))
                    .description("Reimbursement payment")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(claim.getAmount())
                    .build());

            CreateJournalRequest journalRequest = CreateJournalRequest.builder()
                    .journalDate(java.time.LocalDate.now())
                    .description("Reimbursement claim paid: " + claim.getDescription())
                    .journalType(JournalType.GENERAL)
                    .lines(lines)
                    .build();

            JournalResponse created = journalService.create(journalRequest);
            JournalEntry entry = journalEntryRepository.findById(created.getId())
                    .orElseThrow(() -> new BusinessException("Journal not found after creation"));
            entry.setSourceModule("REIMBURSEMENT_PAYMENT");
            entry.setSourceEntityId(claim.getId());
            journalEntryRepository.save(entry);
            journalService.post(created.getId());
        }

        claim.setStatus(ReimbursementStatus.PAID);
        return toResponse(reimbursementClaimRepository.save(claim));
    }

    private void postApprovalJournal(ReimbursementClaim claim) {
        List<CreateJournalLineRequest> lines = new ArrayList<>();
        lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_REIMBURSEMENT_DEFAULT_EXPENSE)))
                .description("Reimbursable expense: " + claim.getDescription())
                .debitAmount(claim.getAmount())
                .creditAmount(BigDecimal.ZERO)
                .build());
        lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountingMappingService.resolve(MappingKey.PAYROLL_REIMBURSEMENT_PAYABLE)))
                .description("Reimbursement owed to " + claim.getEmployee().getFullName())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(claim.getAmount())
                .build());

        CreateJournalRequest journalRequest = CreateJournalRequest.builder()
                .journalDate(claim.getClaimDate())
                .description("Reimbursement claim approved for " + claim.getEmployee().getFullName())
                .journalType(JournalType.GENERAL)
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(journalRequest);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("REIMBURSEMENT_CLAIM");
        entry.setSourceEntityId(claim.getId());
        journalEntryRepository.save(entry);
        journalService.post(created.getId());
    }

    @Transactional(readOnly = true)
    public List<ReimbursementClaimResponse> getByEmployee(Long employeeId) {
        return reimbursementClaimRepository.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReimbursementClaimResponse> getByStatus(ReimbursementStatus status) {
        return reimbursementClaimRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReimbursementClaim getEntity(Long id) {
        return reimbursementClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reimbursement claim not found: " + id));
    }

    private Long resolveAccountId(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .map(a -> Long.valueOf(a.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountCode));
    }

    public ReimbursementClaimResponse toResponse(ReimbursementClaim claim) {
        User approver = claim.getApprovedBy();
        return ReimbursementClaimResponse.builder()
                .id(claim.getId())
                .employeeId(claim.getEmployee().getId())
                .employeeName(claim.getEmployee().getFullName())
                .description(claim.getDescription())
                .category(claim.getCategory())
                .amount(claim.getAmount())
                .claimDate(claim.getClaimDate())
                .status(claim.getStatus())
                .alreadyRecordedElsewhere(claim.isAlreadyRecordedElsewhere())
                .approvedByName(approver != null ? approver.getFullName() : null)
                .approvedAt(claim.getApprovedAt())
                .build();
    }
}
