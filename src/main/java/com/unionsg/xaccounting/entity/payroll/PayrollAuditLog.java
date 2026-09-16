package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.PayrollAuditAction;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit trail for payroll (§44), mirroring the existing
 * {@code FinancialPeriodAuditLog} pattern used for the accounting calendar. Never updated once
 * written.
 */
@Entity
@Table(name = "payroll_audit_logs", indexes = {
        @Index(name = "idx_payroll_audit_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
public class PayrollAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private PayrollAuditEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayrollAuditAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "previous_value", length = 1000)
    private String previousValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Column(length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}
