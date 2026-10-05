package com.unionsg.xaccounting.entity.loan;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.loan.LoanAuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Append-only activity trail for a loan, mirroring {@code PayrollAuditLog}. Never updated once written. */
@Entity
@Table(name = "loan_audit_logs", indexes = {
        @Index(name = "idx_loan_audit_loan", columnList = "loan_id")
})
@Getter
@Setter
public class LoanAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanAuditAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(length = 1000)
    private String details;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}
