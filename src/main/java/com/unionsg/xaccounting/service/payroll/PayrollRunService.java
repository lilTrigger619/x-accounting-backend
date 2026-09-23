package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.payroll.CreatePayrollRunRequest;
import com.unionsg.xaccounting.dto.payroll.EmployeePayrollRecordResponse;
import com.unionsg.xaccounting.dto.payroll.PayrollRecordComponentResponse;
import com.unionsg.xaccounting.dto.payroll.PayrollRunResponse;
import com.unionsg.xaccounting.dto.payroll.ReversePayrollRunRequest;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.payroll.EmployeePayrollRecord;
import com.unionsg.xaccounting.entity.payroll.PayrollCalendarPeriod;
import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.PayrollAuditAction;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.payroll.EmployeePayrollRecordRepository;
import com.unionsg.xaccounting.repository.payroll.PayrollRunRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the Payroll Run lifecycle (§20) - the module's central requirement made concrete.
 * Each transition below is a distinct, separately authorized event; none of them implies the
 * next:
 *
 * <pre>
 *   DRAFT --calculate--> CALCULATED --submitForReview--> UNDER_REVIEW
 *        --approve--> APPROVED --post--> POSTED --pay--> PAID
 *        (POSTED or PAID) --reverse--> REVERSED
 *        (DRAFT/CALCULATED/UNDER_REVIEW) --cancel--> CANCELLED
 * </pre>
 *
 * Reaching CALCULATED never means employees were paid, or even that the GL was touched - only
 * {@code post()} creates a journal, and only {@code pay()} moves cash. {@code approve()} enforces
 * segregation of duties (§22, §45): the same user cannot both prepare and approve a run.
 */
@Service
@RequiredArgsConstructor
public class PayrollRunService {

    private final PayrollRunRepository payrollRunRepository;
    private final EmployeePayrollRecordRepository employeePayrollRecordRepository;
    private final PayrollCalendarService payrollCalendarService;
    private final EmployeeService employeeService;
    private final PayrollCalculationService payrollCalculationService;
    private final PayrollJournalService payrollJournalService;
    private final PayrollPaymentService payrollPaymentService;
    private final PayrollReversalService payrollReversalService;
    private final PayrollInputService payrollInputService;
    private final DocumentNumberService documentNumberGeneratorService;
    private final PayrollAuditLogService auditLogService;

    @Transactional
    public PayrollRunResponse create(CreatePayrollRunRequest request) {
        PayrollCalendarPeriod period = payrollCalendarService.getEntity(request.getPayrollCalendarPeriodId());

        boolean alreadyRunning = payrollRunRepository
                .findByPayrollCalendarPeriodIdAndStatusNot(period.getId(), PayrollRunStatus.CANCELLED).stream()
                .anyMatch(r -> r.getStatus() != PayrollRunStatus.REVERSED);
        if (alreadyRunning) {
            throw new BusinessException("A payroll run already exists for this period (§56 - duplicate payroll run)");
        }

        PayrollRun run = new PayrollRun();
        run.setRunNumber(documentNumberGeneratorService.generateNextNumber(DocumentModule.PAYROLL_RUN));
        run.setPayrollCalendarPeriod(period);
        run.setPayrollGroup(period.getPayrollGroup());
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setPreparedBy(SecurityUtils.getCurrentUser());

        PayrollRun saved = payrollRunRepository.save(run);
        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, saved.getId(), PayrollAuditAction.CREATED,
                null, saved.getRunNumber(), null);

        return toResponse(saved);
    }

    /** Calculates (or recalculates, while not yet POSTED) every active employee in the run's group. */
    @Transactional
    public PayrollRunResponse calculate(Long runId) {
        PayrollRun run = getEntity(runId);
        if (run.getStatus() != PayrollRunStatus.DRAFT && run.getStatus() != PayrollRunStatus.CALCULATED) {
            throw new BusinessException("Only a DRAFT or CALCULATED payroll run can be (re)calculated");
        }

        if (!run.getRecords().isEmpty()) {
            payrollInputService.releaseAppliedForRun(run.getId());
            employeePayrollRecordRepository.deleteAll(run.getRecords());
            run.getRecords().clear();
        }

        List<Employee> employees = employeeService.getActiveEmployeesForGroup(run.getPayrollGroup().getId());
        if (employees.isEmpty()) {
            throw new BusinessException("No active employees belong to payroll group " + run.getPayrollGroup().getName());
        }

        BigDecimal gross = BigDecimal.ZERO, deductions = BigDecimal.ZERO, net = BigDecimal.ZERO, employerCost = BigDecimal.ZERO;
        for (Employee employee : employees) {
            EmployeePayrollRecord record = payrollCalculationService.calculateForEmployee(run, employee);
            run.getRecords().add(record);
            gross = gross.add(record.getGrossPay());
            deductions = deductions.add(record.getTotalEmployeeDeductions());
            net = net.add(record.getNetPay());
            employerCost = employerCost.add(record.getTotalEmployerCost());
        }

        run.setTotalGrossPay(gross.setScale(2, RoundingMode.HALF_UP));
        run.setTotalEmployeeDeductions(deductions.setScale(2, RoundingMode.HALF_UP));
        run.setTotalNetPay(net.setScale(2, RoundingMode.HALF_UP));
        run.setTotalEmployerCost(employerCost.setScale(2, RoundingMode.HALF_UP));
        run.setEmployeeCount(employees.size());
        run.setStatus(PayrollRunStatus.CALCULATED);

        PayrollRun saved = payrollRunRepository.save(run);
        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, saved.getId(), PayrollAuditAction.CALCULATED,
                null, "Net pay " + saved.getTotalNetPay() + " across " + saved.getEmployeeCount() + " employees", null);

        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse submitForReview(Long runId) {
        PayrollRun run = getEntity(runId);
        if (run.getStatus() != PayrollRunStatus.CALCULATED) {
            throw new BusinessException("Only a CALCULATED payroll run can be submitted for review");
        }
        run.setStatus(PayrollRunStatus.UNDER_REVIEW);
        run.setReviewedBy(SecurityUtils.getCurrentUser());
        run.setReviewedAt(LocalDateTime.now());

        PayrollRun saved = payrollRunRepository.save(run);
        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, saved.getId(), PayrollAuditAction.SUBMITTED_FOR_REVIEW,
                null, null, null);
        return toResponse(saved);
    }

    /** Segregation of duties (§22, §45): the approver must be neither the preparer nor the reviewer. */
    @Transactional
    public PayrollRunResponse approve(Long runId) {
        PayrollRun run = getEntity(runId);
        if (run.getStatus() != PayrollRunStatus.UNDER_REVIEW) {
            throw new BusinessException("Only a payroll run UNDER_REVIEW can be approved");
        }
        User approver = SecurityUtils.getCurrentUser();
        if (approver != null && (sameUser(approver, run.getPreparedBy()) || sameUser(approver, run.getReviewedBy()))) {
            throw new BusinessException("Segregation of duties: the preparer or reviewer of a payroll run cannot also approve it");
        }
        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedBy(approver);
        run.setApprovedAt(LocalDateTime.now());

        PayrollRun saved = payrollRunRepository.save(run);
        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, saved.getId(), PayrollAuditAction.APPROVED,
                null, null, null);
        return toResponse(saved);
    }

    /** Posts the run's GL impact (§23-§26). Never implies payment. */
    @Transactional
    public PayrollRunResponse post(Long runId) {
        PayrollRun run = getEntity(runId);
        if (run.getStatus() != PayrollRunStatus.APPROVED) {
            throw new BusinessException("Only an APPROVED payroll run can be posted");
        }

        JournalResponse journal = payrollJournalService.postRun(run);
        run.setJournalId(journal.getId());
        run.setStatus(PayrollRunStatus.POSTED);
        run.setPostedAt(LocalDateTime.now());

        PayrollRun saved = payrollRunRepository.save(run);
        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, saved.getId(), PayrollAuditAction.POSTED,
                null, "Journal #" + journal.getId(), null);
        return toResponse(saved);
    }

    /** Disburses net pay (§27). A distinct event from posting - see class doc. */
    @Transactional
    public PayrollRunResponse pay(Long runId, LocalDate paymentDate) {
        PayrollRun run = getEntity(runId);
        if (run.getStatus() != PayrollRunStatus.POSTED) {
            throw new BusinessException("Only a POSTED payroll run can be paid");
        }

        JournalResponse paymentJournal = payrollPaymentService.payRun(run, paymentDate != null ? paymentDate : LocalDate.now());
        run.setPaymentJournalId(paymentJournal.getId());
        run.setStatus(PayrollRunStatus.PAID);
        run.setPaidAt(LocalDateTime.now());

        PayrollRun saved = payrollRunRepository.save(run);
        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, saved.getId(), PayrollAuditAction.PAID,
                null, "Payment journal #" + paymentJournal.getId(), null);
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse reverse(Long runId, ReversePayrollRunRequest request) {
        PayrollRun run = getEntity(runId);
        payrollReversalService.reverse(run, request.getReason());
        return toResponse(getEntity(runId));
    }

    @Transactional
    public PayrollRunResponse cancel(Long runId) {
        PayrollRun run = getEntity(runId);
        if (run.getStatus() == PayrollRunStatus.POSTED || run.getStatus() == PayrollRunStatus.PAID
                || run.getStatus() == PayrollRunStatus.REVERSED) {
            throw new BusinessException("A posted, paid, or reversed payroll run cannot be cancelled - reverse it instead");
        }
        payrollInputService.releaseAppliedForRun(run.getId());
        run.setStatus(PayrollRunStatus.CANCELLED);

        PayrollRun saved = payrollRunRepository.save(run);
        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, saved.getId(), PayrollAuditAction.CANCELLED,
                null, null, null);
        return toResponse(saved);
    }

    private boolean sameUser(User a, User b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }

    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getAll() {
        return payrollRunRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    /** A single employee's payslip for a run (§39) - exactly the record calculation produced, never recomputed on the fly. */
    @Transactional(readOnly = true)
    public EmployeePayrollRecordResponse getPayslip(Long runId, Long employeeId) {
        EmployeePayrollRecord record = employeePayrollRecordRepository.findByPayrollRunIdAndEmployeeId(runId, employeeId)
                .orElseThrow(() -> new com.unionsg.xaccounting.exception.ResourceNotFoundException(
                        "No payroll record for employee " + employeeId + " in run " + runId));
        return toRecordResponse(record);
    }

    @Transactional(readOnly = true)
    public PayrollRun getEntity(Long id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new com.unionsg.xaccounting.exception.ResourceNotFoundException("Payroll run not found: " + id));
    }

    public PayrollRunResponse toResponse(PayrollRun run) {
        List<EmployeePayrollRecordResponse> records = run.getRecords().stream()
                .map(this::toRecordResponse)
                .toList();

        return PayrollRunResponse.builder()
                .id(run.getId())
                .runNumber(run.getRunNumber())
                .payrollCalendarPeriodId(run.getPayrollCalendarPeriod().getId())
                .payrollGroupName(run.getPayrollGroup().getName())
                .periodLabel(run.getPayrollCalendarPeriod().getPeriodStart() + " to " + run.getPayrollCalendarPeriod().getPeriodEnd())
                .status(run.getStatus())
                .preparedByName(run.getPreparedBy() != null ? run.getPreparedBy().getFullName() : null)
                .reviewedByName(run.getReviewedBy() != null ? run.getReviewedBy().getFullName() : null)
                .reviewedAt(run.getReviewedAt())
                .approvedByName(run.getApprovedBy() != null ? run.getApprovedBy().getFullName() : null)
                .approvedAt(run.getApprovedAt())
                .employeeCount(run.getEmployeeCount())
                .totalGrossPay(run.getTotalGrossPay())
                .totalEmployeeDeductions(run.getTotalEmployeeDeductions())
                .totalNetPay(run.getTotalNetPay())
                .totalEmployerCost(run.getTotalEmployerCost())
                .journalId(run.getJournalId())
                .postedAt(run.getPostedAt())
                .paymentJournalId(run.getPaymentJournalId())
                .paidAt(run.getPaidAt())
                .reversedAt(run.getReversedAt())
                .reversalReason(run.getReversalReason())
                .reversalOfRunId(run.getReversalOfRun() != null ? run.getReversalOfRun().getId() : null)
                .records(records)
                .build();
    }

    private EmployeePayrollRecordResponse toRecordResponse(EmployeePayrollRecord record) {
        Employee employee = record.getEmployee();
        List<PayrollRecordComponentResponse> components = record.getComponents().stream()
                .map(c -> PayrollRecordComponentResponse.builder()
                        .componentName(c.getComponentName())
                        .side(c.getSide())
                        .amount(c.getAmount())
                        .build())
                .toList();

        return EmployeePayrollRecordResponse.builder()
                .id(record.getId())
                .employeeId(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .employeeName(employee.getFullName())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .grossPay(record.getGrossPay())
                .totalEmployeeDeductions(record.getTotalEmployeeDeductions())
                .netPay(record.getNetPay())
                .totalEmployerCost(record.getTotalEmployerCost())
                .negativeNetPayFlagged(record.isNegativeNetPayFlagged())
                .components(components)
                .build();
    }
}
