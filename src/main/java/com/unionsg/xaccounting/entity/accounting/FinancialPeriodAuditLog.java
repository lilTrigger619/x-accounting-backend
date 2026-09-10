package com.unionsg.xaccounting.entity.accounting;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.FinancialPeriodAction;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit trail for every significant status change to the accounting
 * calendar (financial years, periods, opening balances, year-end closing,
 * recurring journals). Never updated once written.
 */
@Entity
@Table(name = "financial_period_audit_logs", indexes = {
        @Index(name = "idx_fp_audit_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
public class FinancialPeriodAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private FinancialPeriodEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinancialPeriodAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", length = 30)
    private String newStatus;

    @Column(length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}
