package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.EmployeePayrollRecordResponse;
import com.unionsg.xaccounting.dto.payroll.PayrollGlReconciliationResponse;
import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
import com.unionsg.xaccounting.projection.ProfitLossAccountProjection;
import com.unionsg.xaccounting.repository.payroll.PayrollRunRepository;
import com.unionsg.xaccounting.repository.reports.LedgerAsOfBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payroll reporting (§41-§43): the Payroll Register (an individual run's line-by-line detail,
 * already served by {@code PayrollRunController#getById}), and the payroll-to-GL reconciliation
 * that proves the subledger and the ledger agree (§25).
 */
@Service
@RequiredArgsConstructor
public class PayrollReportService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunService payrollRunService;
    private final LedgerAsOfBalanceRepository ledgerAsOfBalanceRepository;

    @Value("${payroll.journal.salary-payable-account-id}")
    private String salaryPayableAccountId;

    @Transactional(readOnly = true)
    public List<EmployeePayrollRecordResponse> getRegister(Long runId) {
        return payrollRunService.getById(runId).getRecords();
    }

    @Transactional(readOnly = true)
    public PayrollGlReconciliationResponse getGlReconciliation() {
        LocalDate today = LocalDate.now();

        BigDecimal outstanding = payrollRunRepository.findByStatus(PayrollRunStatus.POSTED).stream()
                .map(PayrollRun::getTotalNetPay)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal glBalance = ledgerAsOfBalanceRepository
                .findAsOfBalances(today, List.of(AccountType.LIABILITY)).stream()
                .filter(p -> salaryPayableAccountId.equals(p.getAccountCode()))
                .map(p -> p.getTotalCredit().subtract(p.getTotalDebit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PayrollGlReconciliationResponse.builder()
                .asOfDate(today)
                .subledgerOutstandingSalaryPayable(outstanding)
                .glSalaryPayableBalance(glBalance)
                .difference(outstanding.subtract(glBalance))
                .build();
    }
}
