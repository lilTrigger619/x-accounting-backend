package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollAuditLog;
import com.unionsg.xaccounting.enums.PayrollAuditAction;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import com.unionsg.xaccounting.repository.payroll.PayrollAuditLogRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Writes the append-only payroll audit trail (§44). Mirrors FinancialPeriodAuditLogService. */
@Service
@RequiredArgsConstructor
public class PayrollAuditLogService {

    private final PayrollAuditLogRepository payrollAuditLogRepository;

    @Transactional
    public void record(PayrollAuditEntityType entityType, Long entityId, PayrollAuditAction action,
                        String previousValue, String newValue, String reason) {
        PayrollAuditLog log = new PayrollAuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setPreviousValue(previousValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        log.setPerformedBy(SecurityUtils.getCurrentUser());
        payrollAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<PayrollAuditLog> getHistory(PayrollAuditEntityType entityType, Long entityId) {
        return payrollAuditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
    }
}
