package com.unionsg.xaccounting.service.accounting;

import com.unionsg.xaccounting.dto.accounting.FinancialPeriodAuditLogResponse;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.accounting.FinancialPeriodAuditLog;
import com.unionsg.xaccounting.enums.FinancialPeriodAction;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import com.unionsg.xaccounting.repository.accounting.FinancialPeriodAuditLogRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Records every significant status change to the accounting calendar. Runs in its
 * own REQUIRES_NEW transaction so a failure to write an audit row can never roll
 * back (or be rolled back by) the business transaction it documents.
 */
@Service
@RequiredArgsConstructor
public class FinancialPeriodAuditLogService {

    private final FinancialPeriodAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            FinancialPeriodEntityType entityType,
            Long entityId,
            FinancialPeriodAction action,
            String previousStatus,
            String newStatus,
            String reason
    ) {
        FinancialPeriodAuditLog log = new FinancialPeriodAuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        User current = SecurityUtils.getCurrentUser();
        log.setPerformedBy(current);
        log.setPerformedAt(LocalDateTime.now());
        log.setPreviousStatus(previousStatus);
        log.setNewStatus(newStatus);
        log.setReason(reason);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<FinancialPeriodAuditLogResponse> getHistory(FinancialPeriodEntityType entityType, Long entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private FinancialPeriodAuditLogResponse toResponse(FinancialPeriodAuditLog log) {
        User user = log.getPerformedBy();
        return FinancialPeriodAuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .performedById(user != null ? user.getId() : null)
                .performedByName(user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "System")
                .performedAt(log.getPerformedAt())
                .previousStatus(log.getPreviousStatus())
                .newStatus(log.getNewStatus())
                .reason(log.getReason())
                .build();
    }
}
