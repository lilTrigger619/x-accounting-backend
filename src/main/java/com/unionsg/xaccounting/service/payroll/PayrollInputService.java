package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollInputRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollInputResponse;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.payroll.PayComponent;
import com.unionsg.xaccounting.entity.payroll.PayrollCalendarPeriod;
import com.unionsg.xaccounting.entity.payroll.PayrollInput;
import com.unionsg.xaccounting.enums.PayrollInputStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.payroll.PayrollInputRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Variable, one-off payroll inputs (§12, §13, §18) - overtime, bonuses, commissions, one-off
 * allowances, ad-hoc deductions/adjustments. Must be {@code APPROVED} before payroll calculation
 * will pick them up (§18); once a Payroll Run actually uses one, it is stamped with that run's id
 * so it can never be pulled into a second run by mistake.
 */
@Service
@RequiredArgsConstructor
public class PayrollInputService {

    private final PayrollInputRepository payrollInputRepository;
    private final EmployeeService employeeService;
    private final PayComponentService payComponentService;
    private final PayrollCalendarService payrollCalendarService;

    @Transactional
    public PayrollInputResponse create(CreatePayrollInputRequest request) {
        Employee employee = employeeService.getEntity(request.getEmployeeId());
        PayrollCalendarPeriod period = payrollCalendarService.getEntity(request.getPayrollCalendarPeriodId());
        PayComponent component = payComponentService.getEntity(request.getPayComponentId());

        PayrollInput input = new PayrollInput();
        input.setEmployee(employee);
        input.setPayrollCalendarPeriod(period);
        input.setSourceType(request.getSourceType());
        input.setPayComponent(component);
        input.setDescription(request.getDescription());
        input.setAmount(request.getAmount());
        input.setHours(request.getHours());
        input.setRate(request.getRate());
        input.setMultiplier(request.getMultiplier());
        input.setStatus(PayrollInputStatus.PENDING);

        return toResponse(payrollInputRepository.save(input));
    }

    @Transactional
    public PayrollInputResponse approve(Long id) {
        PayrollInput input = getEntity(id);
        if (input.getStatus() != PayrollInputStatus.PENDING) {
            throw new BusinessException("Only a pending payroll input can be approved");
        }
        input.setStatus(PayrollInputStatus.APPROVED);
        input.setApprovedBy(SecurityUtils.getCurrentUser());
        input.setApprovedAt(LocalDateTime.now());
        return toResponse(payrollInputRepository.save(input));
    }

    @Transactional
    public PayrollInputResponse reject(Long id) {
        PayrollInput input = getEntity(id);
        if (input.getStatus() != PayrollInputStatus.PENDING) {
            throw new BusinessException("Only a pending payroll input can be rejected");
        }
        input.setStatus(PayrollInputStatus.REJECTED);
        return toResponse(payrollInputRepository.save(input));
    }

    /** Approved, not-yet-applied inputs for a period - what payroll calculation pulls in. */
    @Transactional(readOnly = true)
    public List<PayrollInput> getApprovedForPeriod(Long payrollCalendarPeriodId) {
        return payrollInputRepository.findByPayrollCalendarPeriodIdAndStatus(
                payrollCalendarPeriodId, PayrollInputStatus.APPROVED);
    }

    @Transactional
    public void markApplied(PayrollInput input, Long payrollRunId) {
        input.setStatus(PayrollInputStatus.APPLIED);
        input.setAppliedToRunId(payrollRunId);
        payrollInputRepository.save(input);
    }

    /** Releases inputs a cancelled/recalculated run had applied, back to APPROVED so they can be picked up again. */
    @Transactional
    public void releaseAppliedForRun(Long payrollRunId) {
        List<PayrollInput> applied = payrollInputRepository.findByAppliedToRunId(payrollRunId);
        for (PayrollInput input : applied) {
            input.setStatus(PayrollInputStatus.APPROVED);
            input.setAppliedToRunId(null);
        }
        payrollInputRepository.saveAll(applied);
    }

    @Transactional(readOnly = true)
    public PayrollInput getEntity(Long id) {
        return payrollInputRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll input not found: " + id));
    }

    public PayrollInputResponse toResponse(PayrollInput input) {
        return PayrollInputResponse.builder()
                .id(input.getId())
                .employeeId(input.getEmployee().getId())
                .employeeName(input.getEmployee().getFullName())
                .payrollCalendarPeriodId(input.getPayrollCalendarPeriod().getId())
                .sourceType(input.getSourceType())
                .payComponentId(input.getPayComponent().getId())
                .payComponentName(input.getPayComponent().getName())
                .description(input.getDescription())
                .amount(input.getAmount())
                .hours(input.getHours())
                .rate(input.getRate())
                .multiplier(input.getMultiplier())
                .resolvedAmount(input.resolveAmount())
                .status(input.getStatus())
                .build();
    }
}
