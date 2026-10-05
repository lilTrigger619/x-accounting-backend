package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Append-only trail of everything done to a bank's reconciliation records. */
@Entity
@Table(name = "bank_reconciliation_audit_logs", indexes = {
        @Index(name = "idx_bank_rec_audit_rec", columnList = "reconciliation_id"),
        @Index(name = "idx_bank_rec_audit_account", columnList = "bank_account_id")
})
@Getter
@Setter
public class BankReconciliationAuditLog extends BaseEntity {

    @Column(name = "reconciliation_id")
    private Long reconciliationId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReconciliationAuditAction action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "user_id", length = 40)
    private String userId;

    @Column(name = "user_name", length = 150)
    private String userName;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
