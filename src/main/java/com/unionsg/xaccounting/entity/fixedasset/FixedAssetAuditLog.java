package com.unionsg.xaccounting.entity.fixedasset;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetAuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Append-only trail of asset and depreciation run actions, mirroring {@code LoanAuditLog}. */
@Entity
@Table(name = "fixed_asset_audit_logs", indexes = {
        @Index(name = "idx_fa_audit_asset", columnList = "asset_id"),
        @Index(name = "idx_fa_audit_run", columnList = "run_id")
})
@Getter
@Setter
public class FixedAssetAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "run_id")
    private Long runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FixedAssetAuditAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", length = 30)
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
