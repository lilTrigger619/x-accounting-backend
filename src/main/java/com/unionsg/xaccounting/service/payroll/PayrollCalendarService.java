package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollCalendarPeriodRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollCalendarPeriodResponse;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.payroll.PayrollCalendarPeriod;
import com.unionsg.xaccounting.entity.payroll.PayrollGroup;
import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.enums.PayrollPeriodStatus;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.payroll.PayrollCalendarPeriodRepository;
import com.unionsg.xaccounting.repository.payroll.PayrollRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The Payroll Calendar (§5): the periods a Payroll Group is processed on. Every period is
 * resolved against the application's existing {@link AccountingPeriod}/FinancialYear the moment
 * it's created (§36) - a period with no matching AccountingPeriod is still allowed to exist (the
 * accounting calendar may not have been configured that far out yet), but {@code PayrollRunService}
 * will refuse to post a run against it until one does, via the same {@code PeriodLockGuard} every
 * other posting path in the system respects.
 */
@Service
@RequiredArgsConstructor
public class PayrollCalendarService {

    private final PayrollCalendarPeriodRepository payrollCalendarPeriodRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final PayrollGroupService payrollGroupService;
    private final PayrollRunRepository payrollRunRepository;

    @Transactional
    public PayrollCalendarPeriodResponse create(CreatePayrollCalendarPeriodRequest request) {
        if (request.getPayrollGroupId() == null) {
            throw new BusinessException("Choose a payroll group");
        }
        PayrollGroup group = payrollGroupService.getEntity(request.getPayrollGroupId());
        validateDates(request);
        assertNoOverlap(group.getId(), null, request.getPeriodStart(), request.getPeriodEnd());

        PayrollCalendarPeriod period = new PayrollCalendarPeriod();
        period.setPayrollGroup(group);
        period.setPayFrequency(group.getPayFrequency());
        period.setStatus(PayrollPeriodStatus.OPEN);
        applyDates(period, request);

        return toResponse(payrollCalendarPeriodRepository.save(period));
    }

    /** Changes the dates of an open period that no payroll run uses yet. The group stays the same. */
    @Transactional
    public PayrollCalendarPeriodResponse update(Long id, CreatePayrollCalendarPeriodRequest request) {
        PayrollCalendarPeriod period = getEntity(id);
        assertUnused(period, "edited");
        validateDates(request);
        assertNoOverlap(period.getPayrollGroup().getId(), id, request.getPeriodStart(), request.getPeriodEnd());
        applyDates(period, request);
        return toResponse(payrollCalendarPeriodRepository.save(period));
    }

    /** Soft-deletes an open period that no payroll run uses. */
    @Transactional
    public void delete(Long id) {
        PayrollCalendarPeriod period = getEntity(id);
        assertUnused(period, "deleted");
        period.setDeleted(true);
        period.setDeletedAt(LocalDateTime.now());
        payrollCalendarPeriodRepository.save(period);
    }

    private static void validateDates(CreatePayrollCalendarPeriodRequest request) {
        if (request.getPeriodStart() == null || request.getPeriodEnd() == null
                || request.getPayDate() == null || request.getAccountingDate() == null) {
            throw new BusinessException("Period start, period end, pay date and accounting date are all required");
        }
        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new BusinessException("Period end can't be before period start");
        }
    }

    private void assertNoOverlap(Long groupId, Long ignoreId, LocalDate start, LocalDate end) {
        payrollCalendarPeriodRepository.findByPayrollGroupIdAndDeletedFalseOrderByPeriodStartDesc(groupId).stream()
                .filter(p -> !p.getId().equals(ignoreId))
                .filter(p -> !p.getPeriodStart().isAfter(end) && !p.getPeriodEnd().isBefore(start))
                .findFirst()
                .ifPresent(p -> {
                    throw new BusinessException("These dates overlap the period " + p.getPeriodStart()
                            + " to " + p.getPeriodEnd() + " of this payroll group");
                });
    }

    private void assertUnused(PayrollCalendarPeriod period, String verb) {
        if (period.getStatus() != PayrollPeriodStatus.OPEN) {
            throw new BusinessException("Only open payroll periods can be " + verb);
        }
        if (!payrollRunRepository.findByPayrollCalendarPeriodIdAndStatusNot(period.getId(), PayrollRunStatus.CANCELLED).isEmpty()) {
            throw new BusinessException("This period already has a payroll run, so it can't be " + verb);
        }
    }

    private void applyDates(PayrollCalendarPeriod period, CreatePayrollCalendarPeriodRequest request) {
        period.setPeriodStart(request.getPeriodStart());
        period.setPeriodEnd(request.getPeriodEnd());
        period.setPayDate(request.getPayDate());
        period.setAccountingDate(request.getAccountingDate());

        Optional<AccountingPeriod> accountingPeriod = accountingPeriodRepository.findByDateInRange(request.getAccountingDate());
        period.setAccountingPeriod(accountingPeriod.orElse(null));
        period.setFinancialYear(accountingPeriod.map(AccountingPeriod::getFinancialYear).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<PayrollCalendarPeriodResponse> getByGroup(Long payrollGroupId) {
        return payrollCalendarPeriodRepository.findByPayrollGroupIdAndDeletedFalseOrderByPeriodStartDesc(payrollGroupId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayrollCalendarPeriod getEntity(Long id) {
        return payrollCalendarPeriodRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Payroll calendar period not found: " + id));
    }

    @Transactional
    public void markProcessing(PayrollCalendarPeriod period) {
        period.setStatus(PayrollPeriodStatus.PROCESSING);
        payrollCalendarPeriodRepository.save(period);
    }

    @Transactional
    public void markClosed(PayrollCalendarPeriod period) {
        period.setStatus(PayrollPeriodStatus.CLOSED);
        payrollCalendarPeriodRepository.save(period);
    }

    public PayrollCalendarPeriodResponse toResponse(PayrollCalendarPeriod period) {
        AccountingPeriod ap = period.getAccountingPeriod();
        Optional<PayrollRun> run = period.getId() == null ? Optional.empty()
                : payrollRunRepository.findByPayrollCalendarPeriodIdAndStatusNot(period.getId(), PayrollRunStatus.CANCELLED).stream()
                        .max(Comparator.comparing(PayrollRun::getId));
        return PayrollCalendarPeriodResponse.builder()
                .id(period.getId())
                .payrollGroupId(period.getPayrollGroup().getId())
                .payrollGroupName(period.getPayrollGroup().getName())
                .payFrequency(period.getPayFrequency())
                .periodStart(period.getPeriodStart())
                .periodEnd(period.getPeriodEnd())
                .payDate(period.getPayDate())
                .accountingDate(period.getAccountingDate())
                .accountingPeriodId(ap != null ? ap.getId() : null)
                .accountingPeriodName(ap != null ? ap.getName() : null)
                .financialYearId(period.getFinancialYear() != null ? period.getFinancialYear().getId() : null)
                .financialYearName(period.getFinancialYear() != null ? period.getFinancialYear().getName() : null)
                .status(period.getStatus())
                .payrollRunId(run.map(PayrollRun::getId).orElse(null))
                .payrollRunNumber(run.map(PayrollRun::getRunNumber).orElse(null))
                .payrollRunStatus(run.map(PayrollRun::getStatus).orElse(null))
                .build();
    }
}
