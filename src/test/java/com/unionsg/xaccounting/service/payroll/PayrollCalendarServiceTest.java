package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollCalendarPeriodRequest;
import com.unionsg.xaccounting.entity.payroll.PayrollCalendarPeriod;
import com.unionsg.xaccounting.entity.payroll.PayrollGroup;
import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.enums.PayrollPeriodStatus;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.payroll.PayrollCalendarPeriodRepository;
import com.unionsg.xaccounting.repository.payroll.PayrollRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollCalendarServiceTest {

    @Mock private PayrollCalendarPeriodRepository periodRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private PayrollGroupService payrollGroupService;
    @Mock private PayrollRunRepository payrollRunRepository;

    @InjectMocks
    private PayrollCalendarService service;

    private static PayrollGroup group() {
        PayrollGroup group = new PayrollGroup();
        group.setId(3L);
        group.setName("Monthly staff");
        return group;
    }

    private static CreatePayrollCalendarPeriodRequest request(LocalDate start, LocalDate end) {
        CreatePayrollCalendarPeriodRequest r = new CreatePayrollCalendarPeriodRequest();
        r.setPayrollGroupId(3L);
        r.setPeriodStart(start);
        r.setPeriodEnd(end);
        r.setPayDate(end);
        r.setAccountingDate(end);
        return r;
    }

    private static PayrollCalendarPeriod period(long id, LocalDate start, LocalDate end) {
        PayrollCalendarPeriod p = new PayrollCalendarPeriod();
        p.setId(id);
        p.setPayrollGroup(group());
        p.setPeriodStart(start);
        p.setPeriodEnd(end);
        p.setStatus(PayrollPeriodStatus.OPEN);
        return p;
    }

    @Test
    void rejectsEndBeforeStart() {
        when(payrollGroupService.getEntity(3L)).thenReturn(group());
        assertThatThrownBy(() -> service.create(request(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Period end can't be before period start");
        verify(periodRepository, never()).save(any());
    }

    @Test
    void rejectsOverlappingPeriodInSameGroup() {
        when(payrollGroupService.getEntity(3L)).thenReturn(group());
        when(periodRepository.findByPayrollGroupIdAndDeletedFalseOrderByPeriodStartDesc(3L))
                .thenReturn(List.of(period(9L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))));
        assertThatThrownBy(() -> service.create(request(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 14))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("overlap the period 2026-11-01 to 2026-11-30");
        verify(periodRepository, never()).save(any());
    }

    @Test
    void refusesToDeletePeriodWithPayrollRun() {
        when(periodRepository.findById(9L)).thenReturn(Optional.of(period(9L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))));
        PayrollRun run = new PayrollRun();
        run.setStatus(PayrollRunStatus.DRAFT);
        when(payrollRunRepository.findByPayrollCalendarPeriodIdAndStatusNot(9L, PayrollRunStatus.CANCELLED)).thenReturn(List.of(run));
        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("This period already has a payroll run, so it can't be deleted");
        verify(periodRepository, never()).save(any());
    }

    @Test
    void softDeletesUnusedOpenPeriod() {
        PayrollCalendarPeriod p = period(9L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        when(periodRepository.findById(9L)).thenReturn(Optional.of(p));
        when(payrollRunRepository.findByPayrollCalendarPeriodIdAndStatusNot(9L, PayrollRunStatus.CANCELLED)).thenReturn(List.of());
        service.delete(9L);
        assertThat(p.getDeleted()).isTrue();
        assertThat(p.getDeletedAt()).isNotNull();
        verify(periodRepository).save(p);
    }

    @Test
    void deletedPeriodIsNotFound() {
        PayrollCalendarPeriod p = period(9L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        p.setDeleted(true);
        when(periodRepository.findById(9L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.getEntity(9L)).hasMessageContaining("not found");
    }
}
