package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollAuditLog;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollAuditLogRepository extends JpaRepository<PayrollAuditLog, Long> {
    List<PayrollAuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(PayrollAuditEntityType entityType, Long entityId);
}
