package com.unionsg.xaccounting.service.bankrec;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAuditLog;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAuditAction;
import com.unionsg.xaccounting.repository.bankrec.BankReconciliationAuditLogRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Writes the bank reconciliation audit trail and knows who the current user is. */
@Service
@RequiredArgsConstructor
public class BankRecAuditService {

    private final BankReconciliationAuditLogRepository repository;

    public void record(Long reconciliationId, Long bankAccountId, ReconciliationAuditAction action, String details) {
        BankReconciliationAuditLog log = new BankReconciliationAuditLog();
        log.setReconciliationId(reconciliationId);
        log.setBankAccountId(bankAccountId);
        log.setAction(action);
        log.setDetails(details);
        log.setUserId(currentUserId());
        log.setUserName(currentUserName());
        log.setOccurredAt(LocalDateTime.now());
        repository.save(log);
    }

    public static String currentUserId() {
        User user = SecurityUtils.getCurrentUser();
        return user != null && user.getId() != null ? user.getId().toString() : null;
    }

    public static String currentUserName() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return "System";
        }
        String name = user.getFullName();
        return name != null && !name.isBlank() ? name : user.getEmail();
    }
}
