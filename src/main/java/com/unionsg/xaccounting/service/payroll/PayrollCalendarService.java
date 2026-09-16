package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollCalendarPeriodRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollCalendarPeriodResponse;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.payroll.PayrollCalendarPeriod;
import com.unionsg.xaccounting.entity.payroll.PayrollGroup;
import com.unionsg.xaccounting.enums.PayrollPeriodStatus;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.payroll.PayrollCalendarPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public PayrollCalendarPeriodResponse create(CreatePayrollCalendarPeriodRequest request) {
        PayrollGroup group = payrollGroupService.getEntity(request.getPayrollGroupId());

        PayrollCalendarPeriod period = new PayrollCalendarPeriod();
        period.setPayrollGroup(group);
        period.setPayFrequency(group.getPayFrequency());
        period.setPeriodStart(request.getPeriodStart());
        period.setPeriodEnd(request.getPeriodEnd());
        period.setPayDate(request.getPayDate());
        period.setAccountingDate(request.getAccountingDate());
        period.setStatus(PayrollPeriodStatus.OPEN);

        Optional<AccountingPeriod> accountingPeriod = accountingPeriodRepository.findByDateInRange(request.getAccountingDate());
        accountingPeriod.ifPresent(ap -> {
            period.setAccountingPeriod(ap);
            period.setFinancialYear(ap.getFinancialYear());
        });

        return toResponse(payrollCalendarPeriodRepository.save(period));
    }

    @Transactional(readOnly = true)
    public List<PayrollCalendarPeriodResponse> getByGroup(Long payrollGroupId) {
        return payrollCalendarPeriodRepository.findByPayrollGroupIdOrderByPeriodStartDesc(payrollGroupId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayrollCalendarPeriod getEntity(Long id) {
        return payrollCalendarPeriodRepository.findById(id)
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
                .build();
    }
}
